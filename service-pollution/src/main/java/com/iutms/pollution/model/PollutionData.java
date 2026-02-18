package com.iutms.pollution.model;

import com.iutms.common.model.SensorMessage;

public class PollutionData extends SensorMessage {

    private double co2Level;
    private double noxLevel;
    private double pm25Level;

    public PollutionData() { super("POLLUTION", null, null); }

    public PollutionData(String zoneId, double co2, double nox, double pm25, String timestamp) {
        super("POLLUTION", zoneId, timestamp);
        this.co2Level = co2;
        this.noxLevel = nox;
        this.pm25Level = pm25;
    }

    public double getCo2Level() { return co2Level; }
    public void setCo2Level(double co2Level) { this.co2Level = co2Level; }
    public double getNoxLevel() { return noxLevel; }
    public void setNoxLevel(double noxLevel) { this.noxLevel = noxLevel; }
    public double getPm25Level() { return pm25Level; }
    public void setPm25Level(double pm25Level) { this.pm25Level = pm25Level; }

    @Override
    public String toString() {
        return "PollutionData{zone=" + getZoneId() + ", CO2=" + co2Level +
               ", NOx=" + noxLevel + ", PM2.5=" + pm25Level + "}";
    }
}
