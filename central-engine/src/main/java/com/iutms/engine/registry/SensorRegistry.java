package com.iutms.engine.registry;

import com.iutms.common.discovery.SensorRegistrationRequest;
import com.iutms.common.discovery.ServiceEndpointDescriptor;
import com.iutms.engine.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless sensor registration service.
 *
 * <p>All state lives in MySQL — any Central Engine replica can handle
 * registration and heartbeat requests since they all share the same DB.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate the requested zone exists in {@link ZoneRegistry}</li>
 *   <li>Upsert the sensor row (crash-restart safe)</li>
 *   <li>Return a {@link ServiceEndpointDescriptor} so the sensor knows where to connect</li>
 *   <li>Update {@code last_heartbeat} on ping</li>
 *   <li>Periodically mark timed-out sensors STALE; return them so the engine can alert</li>
 * </ul>
 */
public class SensorRegistry {

    private static final Logger log = LoggerFactory.getLogger(SensorRegistry.class);
    private static final int    HEARTBEAT_INTERVAL_SEC = 30;
    private static final String SELF_URL =
            System.getenv().getOrDefault("CENTRAL_ENGINE_URL", "http://central-engine:8080");

    private final DataSource             dataSource;
    private final ZoneRegistry           zoneRegistry;
    private final ServiceEndpointResolver resolver;

    // ── Stale sensor return type ──────────────────────────────────────────

    /**
     * Carries the sensor ID and its zone so the engine can raise a valid alert.
     */
    public record StaleSensor(String sensorId, String zoneId) {}

    // ── Constructor ───────────────────────────────────────────────────────

    public SensorRegistry(DataSource dataSource,
                          ZoneRegistry zoneRegistry,
                          ServiceEndpointResolver resolver) {
        this.dataSource   = dataSource;
        this.zoneRegistry = zoneRegistry;
        this.resolver     = resolver;
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Register (or re-register) a sensor.
     *
     * <p>Uses {@code INSERT ... ON DUPLICATE KEY UPDATE} so that a sensor
     * that crashed and restarted does not receive a duplicate-key error.
     * The original {@code registered_at} timestamp is preserved.
     *
     * @throws IllegalArgumentException if the requested zone is unknown
     */
    public ServiceEndpointDescriptor register(SensorRegistrationRequest req) {
        if (!zoneRegistry.hasZone(req.getZoneId())) {
            throw new IllegalArgumentException(
                    "Unknown zone: " + req.getZoneId());
        }

        ServiceEndpointDescriptor.EndpointInfo epInfo =
                resolver.resolveForSensor(req.getSensorType(), req.getZoneId());
        if (epInfo == null) {
            throw new IllegalArgumentException(
                    "Unknown sensor type: " + req.getSensorType());
        }

        String host     = epInfo.getHost();
        int    port     = epInfo.getPort();
        String protocol = epInfo.getProtocol();

        String sql = "INSERT INTO sensors " +
                     "(id, sensor_type, zone_id, service_host, service_port, protocol, status, last_heartbeat) " +
                     "VALUES (?,?,?,?,?,?,?,NOW()) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "  status = 'ACTIVE', " +
                     "  last_heartbeat = NOW(), " +
                     "  service_host = VALUES(service_host), " +
                     "  service_port = VALUES(service_port)";

        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, req.getSensorId());
            ps.setString(2, req.getSensorType().toUpperCase());
            ps.setString(3, req.getZoneId());
            ps.setString(4, host);
            ps.setInt(5, port);
            ps.setString(6, protocol);
            ps.setString(7, "ACTIVE");
            ps.executeUpdate();
            log.info("Sensor registered: id={} type={} zone={} proto={}",
                    req.getSensorId(), req.getSensorType(), req.getZoneId(), protocol);
        } catch (SQLException e) {
            log.error("registerSensor failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }

        String heartbeatUrl = SELF_URL + "/api/sensors/" + req.getSensorId() + "/heartbeat";
        return new ServiceEndpointDescriptor(
                req.getSensorId(),
                req.getZoneId(),
                epInfo,
                HEARTBEAT_INTERVAL_SEC,
                heartbeatUrl);
    }

    /**
     * Update {@code last_heartbeat} and ensure status is ACTIVE.
     */
    public void heartbeat(String sensorId) {
        String sql = "UPDATE sensors SET last_heartbeat = NOW(), status = 'ACTIVE' WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sensorId);
            int rows = ps.executeUpdate();
            if (rows == 0) log.warn("Heartbeat for unknown sensor: {}", sensorId);
        } catch (SQLException e) {
            log.error("heartbeat failed for {}: {}", sensorId, e.getMessage());
        }
    }

    /**
     * Mark a sensor as DEREGISTERED on graceful shutdown.
     */
    public void deregister(String sensorId) {
        String sql = "UPDATE sensors SET status = 'DEREGISTERED' WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sensorId);
            ps.executeUpdate();
            log.info("Sensor deregistered: {}", sensorId);
        } catch (SQLException e) {
            log.error("deregister failed for {}: {}", sensorId, e.getMessage());
        }
    }

    /**
     * Background task (every 60s): mark sensors that have not pinged in 90s as STALE.
     *
     * <p>Returns the sensors that just transitioned to STALE so the caller
     * (CentralEngineApp scheduler) can raise an alert per sensor.  The registry
     * itself does not publish alerts — that is the engine's responsibility.
     */
    public List<StaleSensor> markStale() {
        List<StaleSensor> staled = new ArrayList<>();
        // Step 1: find candidates before updating (so we know which IDs changed)
        String selectSql = "SELECT id, zone_id FROM sensors " +
                           "WHERE status = 'ACTIVE' " +
                           "  AND last_heartbeat < NOW() - INTERVAL 90 SECOND";
        try (Connection c = getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(selectSql)) {
            while (rs.next()) {
                staled.add(new StaleSensor(rs.getString("id"), rs.getString("zone_id")));
            }
        } catch (SQLException e) {
            log.error("markStale select failed: {}", e.getMessage());
            return staled;
        }
        if (staled.isEmpty()) return staled;

        // Step 2: update them
        String updateSql = "UPDATE sensors SET status = 'STALE' " +
                           "WHERE status = 'ACTIVE' " +
                           "  AND last_heartbeat < NOW() - INTERVAL 90 SECOND";
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            int updated = s.executeUpdate(updateSql);
            if (updated > 0) log.warn("Marked {} sensor(s) as STALE", updated);
        } catch (SQLException e) {
            log.error("markStale update failed: {}", e.getMessage());
        }
        return staled;
    }

    // ── Query API (for REST endpoints) ────────────────────────────────────

    /**
     * Return all sensor rows as a list of maps (id, sensor_type, zone_id, status, last_heartbeat).
     */
    public List<Map<String, Object>> getAllSensors() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, sensor_type, zone_id, service_host, service_port, " +
                     "protocol, status, registered_at, last_heartbeat FROM sensors " +
                     "ORDER BY registered_at DESC";
        try (Connection c = getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) result.add(rowToMap(rs));
        } catch (SQLException e) {
            log.error("getAllSensors failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Return a single sensor row, or {@code null} if not found.
     */
    public Map<String, Object> getSensorById(String sensorId) {
        String sql = "SELECT id, sensor_type, zone_id, service_host, service_port, " +
                     "protocol, status, registered_at, last_heartbeat FROM sensors WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sensorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToMap(rs);
            }
        } catch (SQLException e) {
            log.error("getSensorById failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Return sensors for a specific zone (for the merged zone+sensors endpoint).
     */
    public List<Map<String, Object>> getSensorsByZone(String zoneId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, sensor_type, zone_id, status, last_heartbeat " +
                     "FROM sensors WHERE zone_id = ? ORDER BY sensor_type, id";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("sensorId",      rs.getString("id"));
                    row.put("sensorType",    rs.getString("sensor_type"));
                    row.put("status",        rs.getString("status"));
                    row.put("lastHeartbeat", rs.getString("last_heartbeat"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("getSensorsByZone failed: {}", e.getMessage());
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : DatabaseConfig.getConnection();
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("sensorId",      rs.getString("id"));
        m.put("sensorType",    rs.getString("sensor_type"));
        m.put("zoneId",        rs.getString("zone_id"));
        m.put("serviceHost",   rs.getString("service_host"));
        m.put("servicePort",   rs.getInt("service_port"));
        m.put("protocol",      rs.getString("protocol"));
        m.put("status",        rs.getString("status"));
        m.put("registeredAt",  rs.getString("registered_at"));
        m.put("lastHeartbeat", rs.getString("last_heartbeat"));
        return m;
    }
}
