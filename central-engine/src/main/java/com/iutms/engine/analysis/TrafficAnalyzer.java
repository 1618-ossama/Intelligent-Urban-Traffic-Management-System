package com.iutms.engine.analysis;

import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core analysis engine that evaluates sensor data against thresholds
 * and generates alerts and recommendations.
 */
public class TrafficAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TrafficAnalyzer.class);
    private final TrafficDAO dao;

    // Thresholds
    private static final double FLOW_CONGESTION_THRESHOLD = 100.0;  // veh/min
    private static final double CO2_ALERT_THRESHOLD = 400.0;        // ppm
    private static final double PM25_ALERT_THRESHOLD = 35.0;        // µg/m³
    private static final double NOISE_ALERT_THRESHOLD = 85.0;       // dB

    public TrafficAnalyzer(TrafficDAO dao) {
        this.dao = dao;
    }

    /**
     * Analyze vehicle flow data. If flow rate exceeds threshold, create congestion alert.
     */
    public void analyzeFlow(String roadId, String zoneId, int count, double flowRate) {
        dao.insertVehicleFlow(roadId, zoneId, count, flowRate);

        if (flowRate > FLOW_CONGESTION_THRESHOLD) {
            Alert alert = new Alert("CONGESTION", zoneId, "HIGH",
                    "Congestion on " + roadId + ": " + flowRate + " veh/min (threshold: " +
                    FLOW_CONGESTION_THRESHOLD + ")");
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                Recommendation rec = new Recommendation(alertId, "EXTEND_GREEN",
                        "Extend green light duration at intersections near " + roadId +
                        " by 15 seconds to reduce congestion");
                dao.insertRecommendation(rec);
                log.warn("ALERT: Congestion on {} ({} veh/min) -> Recommend EXTEND_GREEN", roadId, flowRate);
            }
        }
    }

    /**
     * Analyze pollution data. Alert if CO2 or PM2.5 exceeds thresholds.
     */
    public void analyzePollution(String zoneId, double co2, double nox, double pm25) {
        dao.insertPollution(zoneId, co2, nox, pm25);

        if (co2 > CO2_ALERT_THRESHOLD || pm25 > PM25_ALERT_THRESHOLD) {
            String msg = String.format("High pollution in %s: CO2=%.1f, PM2.5=%.1f", zoneId, co2, pm25);
            Alert alert = new Alert("POLLUTION", zoneId, "HIGH", msg);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                dao.insertRecommendation(new Recommendation(alertId, "REDUCE_TRAFFIC",
                        "Reduce traffic volume in " + zoneId + " by diverting vehicles to alternate routes"));
                log.warn("ALERT: Pollution in {} -> Recommend REDUCE_TRAFFIC", zoneId);
            }
        }
    }

    /**
     * Analyze camera events. Alert on accidents and anomalies.
     */
    public void analyzeCameraEvent(String camId, String zoneId, String eventType,
                                    String severity, String desc) {
        dao.insertCameraEvent(camId, zoneId, eventType, severity, desc);

        if ("ACCIDENT".equals(eventType)) {
            Alert alert = new Alert("ACCIDENT", zoneId, "CRITICAL",
                    "Accident detected by " + camId + ": " + desc);
            long alertId = dao.insertAlert(alert);
            if (alertId > 0) {
                dao.insertRecommendation(new Recommendation(alertId, "DEVIATE",
                        "Activate traffic deviation around " + zoneId + " due to accident on " + camId));
                log.error("ALERT: ACCIDENT detected by {} -> Recommend DEVIATE", camId);
            }
        }
    }

    /**
     * Analyze noise data. Alert if decibel level exceeds threshold.
     */
    public void analyzeNoise(String zoneId, double decibelLevel) {
        dao.insertNoise(zoneId, decibelLevel);

        if (decibelLevel > NOISE_ALERT_THRESHOLD) {
            Alert alert = new Alert("NOISE", zoneId, "MEDIUM",
                    "High noise in " + zoneId + ": " + decibelLevel + " dB");
            dao.insertAlert(alert);
            log.warn("ALERT: Noise in {} ({} dB)", zoneId, decibelLevel);
        }
    }

    /**
     * Analyze signal state changes.
     */
    public void analyzeSignal(String intersectionId, String zoneId, String color, int duration) {
        dao.insertSignalState(intersectionId, zoneId, color, duration);
    }
}
