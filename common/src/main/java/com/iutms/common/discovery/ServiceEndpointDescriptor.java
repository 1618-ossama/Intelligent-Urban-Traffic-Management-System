package com.iutms.common.discovery;

/**
 * Returned by the Central Engine after a successful sensor registration.
 * Tells the sensor where to connect and how to stay alive.
 */
public class ServiceEndpointDescriptor {

    private String       sensorId;
    private String       assignedZone;
    private EndpointInfo endpoint;
    private int          heartbeatIntervalSec;
    private String       heartbeatUrl;

    public ServiceEndpointDescriptor() {}

    public ServiceEndpointDescriptor(String sensorId, String assignedZone,
                                     EndpointInfo endpoint,
                                     int heartbeatIntervalSec, String heartbeatUrl) {
        this.sensorId             = sensorId;
        this.assignedZone         = assignedZone;
        this.endpoint             = endpoint;
        this.heartbeatIntervalSec = heartbeatIntervalSec;
        this.heartbeatUrl         = heartbeatUrl;
    }

    // ── Nested DTO ──────────────────────────────────────────────────────────

    public static class EndpointInfo {
        private String host;
        private int    port;
        private String protocol;
        private String topicHint;
        private int    partitionHint;

        public EndpointInfo() {}

        public EndpointInfo(String host, int port, String protocol,
                            String topicHint, int partitionHint) {
            this.host          = host;
            this.port          = port;
            this.protocol      = protocol;
            this.topicHint     = topicHint;
            this.partitionHint = partitionHint;
        }

        public String getHost()          { return host; }
        public void   setHost(String v)  { this.host = v; }
        public int    getPort()          { return port; }
        public void   setPort(int v)     { this.port = v; }
        public String getProtocol()          { return protocol; }
        public void   setProtocol(String v)  { this.protocol = v; }
        public String getTopicHint()          { return topicHint; }
        public void   setTopicHint(String v)  { this.topicHint = v; }
        public int    getPartitionHint()     { return partitionHint; }
        public void   setPartitionHint(int v) { this.partitionHint = v; }
    }

    // ── Outer getters/setters ───────────────────────────────────────────────

    public String getSensorId()          { return sensorId; }
    public void   setSensorId(String v)  { this.sensorId = v; }

    public String getAssignedZone()          { return assignedZone; }
    public void   setAssignedZone(String v)  { this.assignedZone = v; }

    public EndpointInfo getEndpoint()               { return endpoint; }
    public void         setEndpoint(EndpointInfo v) { this.endpoint = v; }

    public int  getHeartbeatIntervalSec()      { return heartbeatIntervalSec; }
    public void setHeartbeatIntervalSec(int v) { this.heartbeatIntervalSec = v; }

    public String getHeartbeatUrl()          { return heartbeatUrl; }
    public void   setHeartbeatUrl(String v)  { this.heartbeatUrl = v; }
}
