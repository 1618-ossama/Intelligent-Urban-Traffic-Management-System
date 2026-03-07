package com.iutms.engine.analysis;

import com.iutms.engine.config.ThresholdConfig;
import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.model.Alert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrafficAnalyzerTest {

    @Mock
    private TrafficDAO dao;

    private TrafficAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        // Inject known thresholds; no publisher (null = skip Kafka publish in tests)
        ThresholdConfig thresholds = ThresholdConfig.of(100.0, 400.0, 35.0, 85.0);
        analyzer = new TrafficAnalyzer(dao, thresholds);
    }

    // ── Noise ──────────────────────────────────────────────────────────────

    @Test
    void analyzeNoise_atThreshold_noAlert() {
        analyzer.analyzeNoise("zone-center", 85.0);

        verify(dao).insertNoise("zone-center", 85.0);
        verify(dao, never()).insertAlert(any());
    }

    @Test
    void analyzeNoise_aboveThreshold_alertCreated() {
        analyzer.analyzeNoise("zone-center", 85.1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(dao).insertAlert(captor.capture());
        Alert alert = captor.getValue();
        assertEquals("NOISE", alert.getAlertType());
        assertEquals("zone-center", alert.getZoneId());
        assertEquals("MEDIUM", alert.getSeverity());
        assertTrue(alert.getMessage().contains("zone-center"), "Noise alert message should contain zoneId");
    }

    @Test
    void analyzeNoise_zeroValue_noAlert() {
        analyzer.analyzeNoise("zone-center", 0.0);

        verify(dao).insertNoise("zone-center", 0.0);
        verify(dao, never()).insertAlert(any());
    }

    /**
     * Sliding-window spike test: 3 readings below threshold + 1 spike above.
     * Window average (80+80+80+90)/4 = 82.5 < 85 → no alert fired.
     * This is the key new behaviour vs. per-message analysis.
     */
    @Test
    void analyzeNoise_spikeInLowWindow_noAlert() {
        // Use a distinct zone to avoid cross-contamination with other tests
        String zone = "zone-spike-noise";
        analyzer.analyzeNoise(zone, 80.0);
        analyzer.analyzeNoise(zone, 80.0);
        analyzer.analyzeNoise(zone, 80.0);
        analyzer.analyzeNoise(zone, 90.0); // spike: avg = (80+80+80+90)/4 = 82.5 < 85

        verify(dao, never()).insertAlert(any());
    }

    /**
     * Sustained high readings: all 3 readings above threshold → alert fires.
     */
    @Test
    void analyzeNoise_sustainedAboveThreshold_alertFires() {
        when(dao.insertAlert(any())).thenReturn(1L);
        String zone = "zone-sustained-noise";

        analyzer.analyzeNoise(zone, 90.0); // avg=90 > 85 → alert
        analyzer.analyzeNoise(zone, 90.0);
        analyzer.analyzeNoise(zone, 90.0);

        verify(dao, atLeast(1)).insertAlert(any());
    }

    // ── Flow ───────────────────────────────────────────────────────────────

    @Test
    void analyzeFlow_atThreshold_noAlert() {
        analyzer.analyzeFlow("road-1", "zone-north", 50, 100.0);

        verify(dao).insertVehicleFlow("road-1", "zone-north", 50, 100.0);
        verify(dao, never()).insertAlert(any());
    }

    @Test
    void analyzeFlow_aboveThreshold_alertAndRecommendation() {
        when(dao.insertAlert(any())).thenReturn(1L);

        analyzer.analyzeFlow("road-1", "zone-north", 50, 100.1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(dao).insertAlert(captor.capture());
        Alert alert = captor.getValue();
        assertEquals("CONGESTION", alert.getAlertType());
        assertEquals("zone-north", alert.getZoneId());
        assertEquals("HIGH", alert.getSeverity());
        assertTrue(alert.getMessage().contains("road-1"), "Flow alert message should contain roadId");
        verify(dao).insertRecommendation(any());
    }

    @Test
    void analyzeFlow_negativeValue_noAlert() {
        analyzer.analyzeFlow("road-1", "zone-north", 0, -1.0);

        verify(dao).insertVehicleFlow("road-1", "zone-north", 0, -1.0);
        verify(dao, never()).insertAlert(any());
    }

    /**
     * Sliding-window spike for flow: 3 low readings + 1 spike, avg still below threshold.
     */
    @Test
    void analyzeFlow_spikeInLowWindow_noAlert() {
        String zone = "zone-spike-flow";
        analyzer.analyzeFlow("road-x", zone, 10, 50.0);
        analyzer.analyzeFlow("road-x", zone, 10, 50.0);
        analyzer.analyzeFlow("road-x", zone, 10, 50.0);
        analyzer.analyzeFlow("road-x", zone, 10, 200.0); // avg = (50+50+50+200)/4 = 87.5 < 100

        verify(dao, never()).insertAlert(any());
    }

    // ── Pollution ──────────────────────────────────────────────────────────

    @Test
    void analyzePollution_co2AtThreshold_pm25AtThreshold_noAlert() {
        analyzer.analyzePollution("zone-south", 400.0, 0.0, 35.0);

        verify(dao).insertPollution("zone-south", 400.0, 0.0, 35.0);
        verify(dao, never()).insertAlert(any());
    }

    @Test
    void analyzePollution_co2AboveThreshold_alertCreated() {
        when(dao.insertAlert(any())).thenReturn(1L);

        analyzer.analyzePollution("zone-south", 400.1, 0.0, 0.0);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(dao).insertAlert(captor.capture());
        assertEquals("POLLUTION", captor.getValue().getAlertType());
        assertEquals("HIGH", captor.getValue().getSeverity());
        assertTrue(captor.getValue().getMessage().contains("zone-south"), "Pollution alert message should contain zoneId");
    }

    @Test
    void analyzePollution_pm25AboveThreshold_alertCreated() {
        when(dao.insertAlert(any())).thenReturn(1L);

        analyzer.analyzePollution("zone-south", 0.0, 0.0, 35.1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(dao).insertAlert(captor.capture());
        assertEquals("POLLUTION", captor.getValue().getAlertType());
        assertEquals("HIGH", captor.getValue().getSeverity());
    }

    @Test
    void analyzePollution_bothAboveThreshold_singleAlertCreated() {
        when(dao.insertAlert(any())).thenReturn(1L);

        analyzer.analyzePollution("zone-south", 500.0, 0.0, 50.0);

        verify(dao, times(1)).insertAlert(any());
    }

    // ── Camera ─────────────────────────────────────────────────────────────

    @Test
    void analyzeCameraEvent_accidentType_alertCreated() {
        when(dao.insertAlert(any())).thenReturn(1L);

        analyzer.analyzeCameraEvent("cam-01", "zone-east", "ACCIDENT", "CRITICAL", "Collision");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(dao).insertAlert(captor.capture());
        assertEquals("ACCIDENT", captor.getValue().getAlertType());
        assertEquals("CRITICAL", captor.getValue().getSeverity());
        assertTrue(captor.getValue().getMessage().contains("cam-01"), "Camera alert message should contain camId");
        verify(dao).insertRecommendation(any());
    }

    @Test
    void analyzeCameraEvent_nonAccidentType_noAlert() {
        analyzer.analyzeCameraEvent("cam-01", "zone-east", "NORMAL", "LOW", "All clear");

        verify(dao).insertCameraEvent("cam-01", "zone-east", "NORMAL", "LOW", "All clear");
        verify(dao, never()).insertAlert(any());
    }

    // ── Signal ─────────────────────────────────────────────────────────────

    @Test
    void analyzeSignal_callsInsertSignalState() {
        analyzer.analyzeSignal("int-01", "zone-west", "GREEN", 30);

        verify(dao).insertSignalState("int-01", "zone-west", "GREEN", 30);
        verify(dao, never()).insertAlert(any());
    }
}
