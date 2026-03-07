package com.iutms.common.discovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sensor-side HTTP client for the Central Engine discovery API.
 *
 * <p>Sensors call {@link #register} at startup to obtain their assigned
 * service endpoint. They then call {@link #heartbeat} every 30 seconds
 * and {@link #deregister} on graceful shutdown.
 *
 * <p>If the Central Engine is unreachable the sensor falls back to its
 * hardcoded endpoint (existing behaviour) — discovery is additive.
 */
public class DiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryClient.class);
    private static final int TIMEOUT_MS = 5_000;

    private final String centralEngineUrl;
    private final Gson   gson = new GsonBuilder().create();

    public DiscoveryClient() {
        this.centralEngineUrl = System.getenv().getOrDefault(
                "CENTRAL_ENGINE_URL", "http://central-engine:8080");
    }

    public DiscoveryClient(String centralEngineUrl) {
        this.centralEngineUrl = centralEngineUrl;
    }

    /**
     * Self-register with the Central Engine.
     *
     * @return assigned endpoint descriptor, or {@code null} on failure
     */
    public ServiceEndpointDescriptor register(String sensorId, String sensorType, String zoneId) {
        SensorRegistrationRequest req =
                new SensorRegistrationRequest(sensorId, sensorType, zoneId, null);
        String body = gson.toJson(req);
        try {
            HttpURLConnection conn = openConnection(
                    centralEngineUrl + "/api/sensors/register", "POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            if (status == 200 || status == 201) {
                String response = readBody(conn.getInputStream());
                log.info("Sensor {} registered successfully", sensorId);
                return gson.fromJson(response, ServiceEndpointDescriptor.class);
            }
            log.warn("Registration failed for {}: HTTP {}", sensorId, status);
        } catch (Exception e) {
            log.warn("Discovery unavailable, sensor {} will use fallback endpoint: {}",
                    sensorId, e.getMessage());
        }
        return null;
    }

    /**
     * Send a liveness heartbeat.  Fire-and-forget; failure is non-fatal.
     */
    public void heartbeat(String sensorId) {
        try {
            HttpURLConnection conn = openConnection(
                    centralEngineUrl + "/api/sensors/" + encode(sensorId) + "/heartbeat", "POST");
            conn.setRequestProperty("Content-Length", "0");
            int status = conn.getResponseCode();
            if (status != 204 && status != 200) {
                log.debug("Heartbeat {} returned HTTP {}", sensorId, status);
            }
        } catch (Exception e) {
            log.debug("Heartbeat failed for {}: {}", sensorId, e.getMessage());
        }
    }

    /**
     * Graceful deregistration on sensor shutdown.
     */
    public void deregister(String sensorId) {
        try {
            HttpURLConnection conn = openConnection(
                    centralEngineUrl + "/api/sensors/" + encode(sensorId), "DELETE");
            int status = conn.getResponseCode();
            log.info("Deregistered sensor {}: HTTP {}", sensorId, status);
        } catch (Exception e) {
            log.warn("Deregister failed for {}: {}", sensorId, e.getMessage());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private HttpURLConnection openConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        return conn;
    }

    private String readBody(InputStream is) throws Exception {
        byte[] data = is.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    /** Minimal percent-encoding for sensorId path segment (handles spaces). */
    private String encode(String value) {
        return value.replace(" ", "%20");
    }
}
