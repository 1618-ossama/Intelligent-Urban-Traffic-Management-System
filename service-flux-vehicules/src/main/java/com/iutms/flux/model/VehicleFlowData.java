package com.iutms.flux.model;

import com.iutms.common.model.SensorMessage;

/**
 * Data model for vehicle flow sensor readings.
 */
public class VehicleFlowData extends SensorMessage {

    private String roadId;
    private int vehicleCount;
    private double flowRate; // vehicles per minute

    public VehicleFlowData() {
        super("FLUX", null, null);
    }

    public VehicleFlowData(String roadId, String zoneId, int vehicleCount,
                           double flowRate, String timestamp) {
        super("FLUX", zoneId, timestamp);
        this.roadId = roadId;
        this.vehicleCount = vehicleCount;
        this.flowRate = flowRate;
    }

    public String getRoadId() { return roadId; }
    public void setRoadId(String roadId) { this.roadId = roadId; }
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }
    public double getFlowRate() { return flowRate; }
    public void setFlowRate(double flowRate) { this.flowRate = flowRate; }

    @Override
    public String toString() {
        return "VehicleFlowData{road=" + roadId + ", count=" + vehicleCount +
               ", rate=" + flowRate + ", zone=" + getZoneId() + "}";
    }
}
