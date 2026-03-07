package com.iutms.engine.analysis;

import com.iutms.engine.alert.AlertPublisher;
import com.iutms.engine.config.ThresholdConfig;
import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core analysis engine that evaluates sensor data against thresholds
 * and generates alerts and recommendations.
 *
 * <p>Each metric (flow, CO2, PM2.5, noise) is tracked through a 5-minute
 * sliding window. Alerts fire only when the window average exceeds the
 * configured threshold — not on isolated spikes.
 *
 * <p>Generated alerts are persisted via {@link TrafficDAO} and, when an
 * {@link AlertPublisher} is provided, also published to Kafka for downstream
 * closed-loop control (e.g. service-feux signal adjustment).
 */
public class TrafficAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TrafficAnalyzer.class);

    private final TrafficDAO dao;
    private final ThresholdConfig thresholds;
    private final AlertPublisher publisher;
    private final SlidingWindow flowWindow  = new SlidingWindow(300_000);
    private final SlidingWindow co2Window   = new SlidingWindow(300_000);
    private final SlidingWindow pm25Window  = new SlidingWindow(300_000);
    private final SlidingWindow noiseWindow = new SlidingWindow(300_000);

    public TrafficAnalyzer(TrafficDAO dao, ThresholdConfig thresholds) {
        this(dao, thresholds, null);
    }

    public TrafficAnalyzer(TrafficDAO dao, ThresholdConfig thresholds, AlertPublisher publisher) {
        this.dao       = dao;
        this.thresholds = thresholds;
        this.publisher  = publisher;
    }

    /**
     * Analyze vehicle flow data. Alert if the 5-minute window average exceeds threshold.
     */
    public void analyzeFlow(String roadId, String zoneId, int count, double flowRate) {
        dao.insertVehicleFlow(roadId, zoneId, count, flowRate);

        double avg = flowWindow.add(zoneId, flowRate);
        if (avg > thresholds.getFlow()) {
            Alert alert = new Alert("CONGESTION", zoneId, "HIGH",
                    "Congestion on " + roadId + ": avg " + String.format("%.1f", avg) +
                    " veh/min (threshold: " + thresholds.getFlow() + ")");
            double pct = avg / thresholds.getFlow() * 100.0;
            alert.setPercentOfThreshold(pct);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                dao.insertRecommendation(new Recommendation(alertId, "EXTEND_GREEN",
                        "Extend green light duration at intersections near " + roadId +
                        " by 15 seconds to reduce congestion"));
                if (publisher != null) publisher.publish(zoneId, "CONGESTION", "HIGH", pct);
                log.warn("ALERT: Congestion on {} (avg {} veh/min) -> Recommend EXTEND_GREEN",
                        roadId, String.format("%.1f", avg));
            }
        }
    }

    /**
     * Analyze pollution data. Alert if the 5-minute CO2 or PM2.5 window average exceeds threshold.
     */
    public void analyzePollution(String zoneId, double co2, double nox, double pm25) {
        dao.insertPollution(zoneId, co2, nox, pm25);

        double avgCo2  = co2Window.add(zoneId, co2);
        double avgPm25 = pm25Window.add(zoneId, pm25);

        if (avgCo2 > thresholds.getCo2() || avgPm25 > thresholds.getPm25()) {
            String msg = String.format("High pollution in %s: CO2=%.1f, PM2.5=%.1f", zoneId, avgCo2, avgPm25);
            Alert alert = new Alert("POLLUTION", zoneId, "HIGH", msg);
            double pct = Math.max(avgCo2 / thresholds.getCo2(), avgPm25 / thresholds.getPm25()) * 100.0;
            alert.setPercentOfThreshold(pct);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                dao.insertRecommendation(new Recommendation(alertId, "REDUCE_TRAFFIC",
                        "Reduce traffic volume in " + zoneId + " by diverting vehicles to alternate routes"));
                if (publisher != null) publisher.publish(zoneId, "POLLUTION", "HIGH", pct);
                log.warn("ALERT: Pollution in {} -> Recommend REDUCE_TRAFFIC", zoneId);
            }
        }
    }

    /**
     * Analyze camera events. Alert on accidents (event-driven — no windowing).
     */
    public void analyzeCameraEvent(String camId, String zoneId, String eventType,
                                    String severity, String desc) {
        dao.insertCameraEvent(camId, zoneId, eventType, severity, desc);

        if ("ACCIDENT".equals(eventType)) {
            Alert alert = new Alert("ACCIDENT", zoneId, "CRITICAL",
                    "Accident detected by " + camId + ": " + desc);
            alert.setPercentOfThreshold(200.0);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                dao.insertRecommendation(new Recommendation(alertId, "DEVIATE",
                        "Activate traffic deviation around " + zoneId + " due to accident on " + camId));
                if (publisher != null) publisher.publish(zoneId, "ACCIDENT", "CRITICAL", 200.0);
                log.error("ALERT: ACCIDENT detected by {} -> Recommend DEVIATE", camId);
            }
        }
    }

    /**
     * Analyze noise data. Alert if the 5-minute window average exceeds threshold.
     */
    public void analyzeNoise(String zoneId, double decibelLevel) {
        dao.insertNoise(zoneId, decibelLevel);

        double avg = noiseWindow.add(zoneId, decibelLevel);
        if (avg > thresholds.getNoise()) {
            Alert alert = new Alert("NOISE", zoneId, "MEDIUM",
                    "High noise in " + zoneId + ": avg " + String.format("%.1f", avg) + " dB");
            double pct = avg / thresholds.getNoise() * 100.0;
            alert.setPercentOfThreshold(pct);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0 && publisher != null) {
                publisher.publish(zoneId, "NOISE", "MEDIUM", pct);
            }
            log.warn("ALERT: Noise in {} (avg {} dB)", zoneId, String.format("%.1f", avg));
        }
    }

    /**
     * Analyze signal state changes (event-driven — no windowing).
     */
    public void analyzeSignal(String intersectionId, String zoneId, String color, int duration) {
        dao.insertSignalState(intersectionId, zoneId, color, duration);
    }

    /**
     * Raise a SENSOR_STALE alert for a sensor that has missed heartbeats.
     * Called by the CentralEngineApp markStale scheduler.
     */
    public void raiseSensorStale(String sensorId, String zoneId) {
        Alert alert = new Alert("SENSOR_STALE", zoneId, "MEDIUM",
                "Sensor " + sensorId + " in " + zoneId + " has stopped sending heartbeats");
        alert.setPercentOfThreshold(0.0);
        long alertId = dao.insertAlert(alert);
        if (alertId > 0 && publisher != null) {
            publisher.publish(zoneId, "SENSOR_STALE", "MEDIUM", 0.0);
        }
        log.warn("STALE SENSOR: {} in zone {}", sensorId, zoneId);
    }
}
