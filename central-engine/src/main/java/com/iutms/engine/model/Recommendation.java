package com.iutms.engine.model;

public class Recommendation {
    private long id;
    private long alertId;
    private String actionType; // EXTEND_GREEN, REDUCE_TRAFFIC, DEVIATE, SYNC_SIGNALS
    private String description;
    private String status;     // PENDING, APPLIED, DISMISSED
    private String createdAt;

    public Recommendation() {}

    public Recommendation(long alertId, String actionType, String description) {
        this.alertId = alertId;
        this.actionType = actionType;
        this.description = description;
        this.status = "PENDING";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getAlertId() { return alertId; }
    public void setAlertId(long alertId) { this.alertId = alertId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
