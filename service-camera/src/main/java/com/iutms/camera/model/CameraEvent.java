package com.iutms.camera.model;

import com.iutms.common.model.SensorMessage;

public class CameraEvent extends SensorMessage {

    private String cameraId;
    private String eventType;  // NORMAL, ACCIDENT, ANOMALY, CONGESTION
    private String severity;   // LOW, MEDIUM, HIGH, CRITICAL
    private String description;

    public CameraEvent() { super("CAMERA", null, null); }

    public CameraEvent(String cameraId, String zoneId, String eventType,
                       String severity, String description, String timestamp) {
        super("CAMERA", zoneId, timestamp);
        this.cameraId = cameraId;
        this.eventType = eventType;
        this.severity = severity;
        this.description = description;
    }

    public String getCameraId() { return cameraId; }
    public void setCameraId(String cameraId) { this.cameraId = cameraId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isIncident() {
        return !"NORMAL".equals(eventType);
    }

    @Override
    public String toString() {
        return "CameraEvent{cam=" + cameraId + ", type=" + eventType +
               ", severity=" + severity + ", zone=" + getZoneId() + "}";
    }
}
