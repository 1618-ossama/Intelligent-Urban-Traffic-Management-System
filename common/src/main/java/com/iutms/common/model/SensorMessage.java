package com.iutms.common.model;

import java.io.Serializable;

/**
 * Base class for all sensor messages sent through Kafka.
 * Every service message includes these common fields.
 */
public class SensorMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String serviceType;  // FLUX, POLLUTION, CAMERA, NOISE, SIGNAL
    private String zoneId;
    private String timestamp;

    public SensorMessage() {}

    public SensorMessage(String serviceType, String zoneId, String timestamp) {
        this.serviceType = serviceType;
        this.zoneId = zoneId;
        this.timestamp = timestamp;
    }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return serviceType + "[zone=" + zoneId + ", time=" + timestamp + "]";
    }
}
