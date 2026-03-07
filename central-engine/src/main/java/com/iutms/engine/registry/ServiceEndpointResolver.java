package com.iutms.engine.registry;

import com.iutms.common.discovery.ServiceEndpointDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps sensor types to their service endpoints, reading host/port from
 * environment variables with static defaults as fallback.
 *
 * <p>Environment variable convention:
 * <ul>
 *   <li>{@code SERVICE_NOISE_HOST} / {@code SERVICE_NOISE_PORT}
 *   <li>{@code SERVICE_POLLUTION_HOST} / {@code SERVICE_POLLUTION_PORT}
 *   <li>{@code SERVICE_CAMERA_HOST} / {@code SERVICE_CAMERA_PORT}
 *   <li>{@code SERVICE_FLUX_HOST} / {@code SERVICE_FLUX_PORT}
 *   <li>{@code SERVICE_SIGNAL_HOST} / {@code SERVICE_SIGNAL_PORT}
 * </ul>
 */
public class ServiceEndpointResolver {

    private static final Logger log = LoggerFactory.getLogger(ServiceEndpointResolver.class);
    private static final int NUM_PARTITIONS = 3;

    private enum SensorType { NOISE, POLLUTION, CAMERA, FLUX, SIGNAL }

    private record ServiceEndpoint(String host, int port, String protocol) {}

    private final Map<SensorType, ServiceEndpoint> endpoints;
    private final ZoneRegistry zoneRegistry;

    public ServiceEndpointResolver(ZoneRegistry zoneRegistry) {
        this.zoneRegistry = zoneRegistry;
        endpoints = new EnumMap<>(SensorType.class);
        endpoints.put(SensorType.NOISE,      resolve("NOISE",      "service-bruit",          9090, "TCP"));
        endpoints.put(SensorType.POLLUTION,  resolve("POLLUTION",  "service-pollution",      8082, "REST"));
        endpoints.put(SensorType.CAMERA,     resolve("CAMERA",     "service-camera",         1099, "RMI"));
        endpoints.put(SensorType.FLUX,       resolve("FLUX",       "service-flux-vehicules", 8081, "SOAP_WS"));
        endpoints.put(SensorType.SIGNAL,     resolve("SIGNAL",     "service-feux",           8084, "SOAP_RPC"));
        log.info("ServiceEndpointResolver initialised with {} sensor types", endpoints.size());
    }

    /**
     * Resolve the assigned endpoint for a given sensor type and zone.
     *
     * @return populated {@link ServiceEndpointDescriptor.EndpointInfo}, or {@code null} for unknown types
     */
    public ServiceEndpointDescriptor.EndpointInfo resolveForSensor(String sensorType, String zoneId) {
        SensorType type;
        try {
            type = SensorType.valueOf(sensorType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown sensor type: {}", sensorType);
            return null;
        }
        ServiceEndpoint ep = endpoints.get(type);
        if (ep == null) return null;

        int    partitionHint = zoneRegistry.partitionFor(zoneId, NUM_PARTITIONS);
        String topicHint     = topicFor(type);

        return new ServiceEndpointDescriptor.EndpointInfo(
                ep.host(), ep.port(), ep.protocol(), topicHint, partitionHint);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ServiceEndpoint resolve(String type, String defHost, int defPort, String proto) {
        String host = System.getenv().getOrDefault("SERVICE_" + type + "_HOST", defHost);
        int port = Integer.parseInt(
                System.getenv().getOrDefault("SERVICE_" + type + "_PORT",
                        String.valueOf(defPort)));
        return new ServiceEndpoint(host, port, proto);
    }

    private String topicFor(SensorType type) {
        return switch (type) {
            case NOISE      -> "noise-data";
            case POLLUTION  -> "pollution-data";
            case CAMERA     -> "camera-events";
            case FLUX       -> "traffic-flow";
            case SIGNAL     -> "signal-events";
        };
    }
}
