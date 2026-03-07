package com.iutms.engine.model;

public class Alert {
    private long id;
    private String alertType;  // CONGESTION, POLLUTION, ACCIDENT, NOISE, RUSH_HOUR
    private String zoneId;
    private String severity;   // LOW, MEDIUM, HIGH, CRITICAL
    private String message;
    private boolean active;
    private String createdAt;
    private double percentOfThreshold;

    public Alert() {}

    public Alert(String alertType, String zoneId, String severity, String message) {
        this.alertType = alertType;
        this.zoneId = zoneId;
        this.severity = severity;
        this.message = message;
        this.active = true;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public double getPercentOfThreshold() { return percentOfThreshold; }
    public void setPercentOfThreshold(double percentOfThreshold) { this.percentOfThreshold = percentOfThreshold; }
}
