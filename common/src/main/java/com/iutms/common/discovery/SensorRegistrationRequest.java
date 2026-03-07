package com.iutms.common.discovery;

/**
 * Payload sent by a sensor when self-registering with the Central Engine.
 */
public class SensorRegistrationRequest {

    private String sensorId;
    private String sensorType;        // NOISE | POLLUTION | CAMERA | FLUX | SIGNAL
    private String zoneId;
    private String preferredProtocol; // optional hint; server may ignore

    public SensorRegistrationRequest() {}

    public SensorRegistrationRequest(String sensorId, String sensorType,
                                     String zoneId, String preferredProtocol) {
        this.sensorId          = sensorId;
        this.sensorType        = sensorType;
        this.zoneId            = zoneId;
        this.preferredProtocol = preferredProtocol;
    }

    public String getSensorId()          { return sensorId; }
    public void   setSensorId(String v)  { this.sensorId = v; }

    public String getSensorType()          { return sensorType; }
    public void   setSensorType(String v)  { this.sensorType = v; }

    public String getZoneId()          { return zoneId; }
    public void   setZoneId(String v)  { this.zoneId = v; }

    public String getPreferredProtocol()          { return preferredProtocol; }
    public void   setPreferredProtocol(String v)  { this.preferredProtocol = v; }
}
