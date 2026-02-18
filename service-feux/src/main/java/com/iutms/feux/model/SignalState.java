package com.iutms.feux.model;

import com.iutms.common.model.SensorMessage;

public class SignalState extends SensorMessage {

    private String intersectionId;
    private String color;  // RED, GREEN, YELLOW
    private int durationSec;

    public SignalState() { super("SIGNAL", null, null); }

    public SignalState(String intersectionId, String zoneId, String color,
                       int durationSec, String timestamp) {
        super("SIGNAL", zoneId, timestamp);
        this.intersectionId = intersectionId;
        this.color = color;
        this.durationSec = durationSec;
    }

    public String getIntersectionId() { return intersectionId; }
    public void setIntersectionId(String id) { this.intersectionId = id; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getDurationSec() { return durationSec; }
    public void setDurationSec(int durationSec) { this.durationSec = durationSec; }

    @Override
    public String toString() {
        return "Signal{" + intersectionId + "=" + color + "/" + durationSec + "s}";
    }
}
