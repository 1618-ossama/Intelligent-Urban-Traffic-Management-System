package com.iutms.camera.simulator;

import com.iutms.common.config.GeographyConfig;
import com.iutms.camera.model.CameraEvent;
import com.iutms.common.util.TimeUtil;

import java.util.*;

public class CameraSimulator {

    private final Random random = new Random();
    private static final String[] EVENT_TYPES = {"NORMAL", "NORMAL", "NORMAL", "ACCIDENT", "ANOMALY", "CONGESTION"};
    private final List<String> cameras;

    public CameraSimulator() {
        this.cameras = GeographyConfig.getInstance().getCameras();
    }

    public CameraEvent getStatus(String cameraId, String zoneId) {
        String type = EVENT_TYPES[random.nextInt(EVENT_TYPES.length)];
        String severity = type.equals("NORMAL") ? "LOW" :
                          type.equals("ACCIDENT") ? "CRITICAL" : "MEDIUM";
        String desc = type.equals("NORMAL") ? "Normal traffic flow" :
                      type.equals("ACCIDENT") ? "Vehicle collision detected" :
                      type.equals("CONGESTION") ? "Heavy traffic buildup" : "Unusual movement pattern";
        return new CameraEvent(cameraId, zoneId, type, severity, desc, TimeUtil.now());
    }

    public CameraEvent detectIncident(String cameraId, String zoneId) {
        // 20% chance of incident
        boolean incident = random.nextInt(5) == 0;
        if (incident) {
            String[] types = {"ACCIDENT", "ANOMALY", "CONGESTION"};
            String type = types[random.nextInt(types.length)];
            String severity = type.equals("ACCIDENT") ? "CRITICAL" : "HIGH";
            return new CameraEvent(cameraId, zoneId, type, severity,
                    "Incident detected by camera " + cameraId, TimeUtil.now());
        }
        return new CameraEvent(cameraId, zoneId, "NORMAL", "LOW",
                "No incident", TimeUtil.now());
    }

    public List<CameraEvent> getZoneEvents(String zoneId) {
        List<CameraEvent> events = new ArrayList<>();
        for (String cam : cameras) {
            events.add(getStatus(cam, zoneId));
        }
        return events;
    }
}
