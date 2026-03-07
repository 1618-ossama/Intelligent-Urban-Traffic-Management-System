package com.iutms.bruit.model;

import com.iutms.common.model.SensorMessage;

public class NoiseData extends SensorMessage {

    private double decibelLevel;

    public NoiseData() { super("NOISE", null, null); }

    public NoiseData(String zoneId, double decibelLevel, String timestamp) {
        super("NOISE", zoneId, timestamp);
        this.decibelLevel = decibelLevel;
    }

    public double getDecibelLevel() { return decibelLevel; }
    public void setDecibelLevel(double decibelLevel) { this.decibelLevel = decibelLevel; }

    @Override
    public String toString() {
        return "NoiseData{zone=" + getZoneId() + ", dB=" + decibelLevel + "}";
    }
}
