package com.iutms.engine.dao;

import com.iutms.engine.config.DatabaseConfig;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for persisting sensor data, alerts, and recommendations.
 */
public class TrafficDAO {

    private static final Logger log = LoggerFactory.getLogger(TrafficDAO.class);
    private final DataSource dataSource;

    public TrafficDAO() { this.dataSource = null; }

    TrafficDAO(DataSource ds) { this.dataSource = ds; }

    private Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : DatabaseConfig.getConnection();
    }

    public void insertVehicleFlow(String roadId, String zoneId, int count, double rate) {
        String sql = "INSERT INTO vehicle_flow (road_id, zone_id, vehicle_count, flow_rate) VALUES (?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roadId); ps.setString(2, zoneId);
            ps.setInt(3, count); ps.setDouble(4, rate);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert vehicle_flow failed: {}", e.getMessage()); }
    }

    public void insertPollution(String zoneId, double co2, double nox, double pm25) {
        String sql = "INSERT INTO pollution_data (zone_id, co2_level, nox_level, pm25_level) VALUES (?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId); ps.setDouble(2, co2);
            ps.setDouble(3, nox); ps.setDouble(4, pm25);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert pollution_data failed: {}", e.getMessage()); }
    }

    public void insertCameraEvent(String camId, String zoneId, String type, String severity, String desc) {
        String sql = "INSERT INTO camera_events (camera_id, zone_id, event_type, severity, description) VALUES (?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, camId); ps.setString(2, zoneId); ps.setString(3, type);
            ps.setString(4, severity); ps.setString(5, desc);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert camera_events failed: {}", e.getMessage()); }
    }

    public void insertNoise(String zoneId, double dB) {
        String sql = "INSERT INTO noise_data (zone_id, decibel_level) VALUES (?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId); ps.setDouble(2, dB);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert noise_data failed: {}", e.getMessage()); }
    }

    public void insertSignalState(String intId, String zoneId, String color, int duration) {
        String sql = "INSERT INTO signal_states (intersection_id, zone_id, color, duration_sec) VALUES (?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, intId); ps.setString(2, zoneId);
            ps.setString(3, color); ps.setInt(4, duration);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert signal_states failed: {}", e.getMessage()); }
    }

    public long insertAlert(Alert alert) {
        String sql = "INSERT INTO alerts (alert_type, zone_id, severity, message, percent_of_threshold) VALUES (?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alert.getAlertType()); ps.setString(2, alert.getZoneId());
            ps.setString(3, alert.getSeverity()); ps.setString(4, alert.getMessage());
            ps.setDouble(5, alert.getPercentOfThreshold());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { log.error("Insert alert failed: {}", e.getMessage()); }
        return -1;
    }

    public void insertRecommendation(Recommendation rec) {
        String sql = "INSERT INTO recommendations (alert_id, action_type, description) VALUES (?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, rec.getAlertId()); ps.setString(2, rec.getActionType());
            ps.setString(3, rec.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert recommendation failed: {}", e.getMessage()); }
    }

    public List<Alert> getActiveAlerts() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE is_active = TRUE ORDER BY created_at DESC LIMIT 50";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Alert a = new Alert();
                a.setId(rs.getLong("id")); a.setAlertType(rs.getString("alert_type"));
                a.setZoneId(rs.getString("zone_id")); a.setSeverity(rs.getString("severity"));
                a.setMessage(rs.getString("message")); a.setActive(rs.getBoolean("is_active"));
                a.setCreatedAt(rs.getString("created_at"));
                a.setPercentOfThreshold(rs.getDouble("percent_of_threshold"));
                alerts.add(a);
            }
        } catch (SQLException e) { log.error("Query alerts failed: {}", e.getMessage()); }
        return alerts;
    }

    public List<Recommendation> getRecentRecommendations() {
        List<Recommendation> recs = new ArrayList<>();
        String sql = "SELECT * FROM recommendations ORDER BY created_at DESC LIMIT 50";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Recommendation r = new Recommendation();
                r.setId(rs.getLong("id")); r.setAlertId(rs.getLong("alert_id"));
                r.setActionType(rs.getString("action_type")); r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status")); r.setCreatedAt(rs.getString("created_at"));
                recs.add(r);
            }
        } catch (SQLException e) { log.error("Query recommendations failed: {}", e.getMessage()); }
        return recs;
    }

    public void acknowledgeAlert(long id) {
        String sql = "UPDATE alerts SET is_active = FALSE, resolved_at = NOW() WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Acknowledge alert failed: {}", e.getMessage()); }
    }

    // ── Zone config queries (for /api/zones/config endpoints) ────────────

    public List<Map<String, Object>> getZoneConfigs() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, name, latitude, longitude, area_sq_km, radius_m, partition_idx " +
                     "FROM zones WHERE is_active = TRUE ORDER BY name";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) result.add(zoneConfigRow(rs));
        } catch (SQLException e) { log.error("getZoneConfigs failed: {}", e.getMessage()); }
        return result;
    }

    public Map<String, Object> getZoneConfig(String zoneId) {
        String sql = "SELECT id, name, latitude, longitude, area_sq_km, radius_m, partition_idx " +
                     "FROM zones WHERE id = ? AND is_active = TRUE";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return zoneConfigRow(rs);
            }
        } catch (SQLException e) { log.error("getZoneConfig failed: {}", e.getMessage()); }
        return null;
    }

    private Map<String, Object> zoneConfigRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id",           rs.getString("id"));
        m.put("name",         rs.getString("name"));
        m.put("lat",          rs.getDouble("latitude"));
        m.put("lng",          rs.getDouble("longitude"));
        m.put("areaSqKm",     rs.getDouble("area_sq_km"));
        m.put("radius_m",     rs.getInt("radius_m"));
        m.put("partition_idx", rs.getInt("partition_idx"));
        return m;
    }

    public List<Map<String, Object>> getLatestTrafficByRoad() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT road_id, ROUND(AVG(vehicle_count)) AS vehicle_count, " +
                     "ROUND(AVG(flow_rate), 1) AS flow_rate " +
                     "FROM vehicle_flow WHERE recorded_at >= NOW() - INTERVAL 5 MINUTE " +
                     "GROUP BY road_id ORDER BY road_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("roadId", rs.getString("road_id"));
                row.put("vehicleCount", rs.getInt("vehicle_count"));
                row.put("flowRate", rs.getDouble("flow_rate"));
                result.add(row);
            }
        } catch (SQLException e) { log.error("getLatestTrafficByRoad failed: {}", e.getMessage()); }
        return result;
    }

    public List<Map<String, Object>> getLatestPollutionByZone() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT zone_id, ROUND(AVG(co2_level), 1) AS co2, " +
                     "ROUND(AVG(nox_level), 1) AS nox, ROUND(AVG(pm25_level), 1) AS pm25 " +
                     "FROM pollution_data WHERE recorded_at >= NOW() - INTERVAL 5 MINUTE " +
                     "GROUP BY zone_id ORDER BY zone_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("zoneId", rs.getString("zone_id"));
                row.put("co2", rs.getDouble("co2"));
                row.put("nox", rs.getDouble("nox"));
                row.put("pm25", rs.getDouble("pm25"));
                result.add(row);
            }
        } catch (SQLException e) { log.error("getLatestPollutionByZone failed: {}", e.getMessage()); }
        return result;
    }

    public List<Map<String, Object>> getLatestSignalStates() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT ss.intersection_id, ss.color, ss.duration_sec " +
                     "FROM signal_states ss " +
                     "INNER JOIN (SELECT intersection_id, MAX(recorded_at) AS max_ts " +
                     "            FROM signal_states GROUP BY intersection_id) latest " +
                     "ON ss.intersection_id = latest.intersection_id AND ss.recorded_at = latest.max_ts " +
                     "ORDER BY ss.intersection_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("intersectionId", rs.getString("intersection_id"));
                row.put("color", rs.getString("color"));
                row.put("durationSec", rs.getInt("duration_sec"));
                result.add(row);
            }
        } catch (SQLException e) { log.error("getLatestSignalStates failed: {}", e.getMessage()); }
        return result;
    }

    public Map<String, Map<String, Object>> getZoneSummary() {
        Map<String, Map<String, Object>> result = new java.util.HashMap<>();

        // Latest avg noise per zone (last 5 minutes)
        String noiseSql = "SELECT zone_id, AVG(decibel_level) AS avg_noise " +
                          "FROM noise_data WHERE recorded_at >= NOW() - INTERVAL 5 MINUTE GROUP BY zone_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(noiseSql)) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("zone_id"), k -> new java.util.HashMap<>())
                      .put("noise", rs.getDouble("avg_noise"));
            }
        } catch (SQLException e) { log.error("Zone noise query failed: {}", e.getMessage()); }

        // Latest avg flow rate per zone (last 5 minutes)
        String flowSql = "SELECT zone_id, AVG(flow_rate) AS avg_flow " +
                         "FROM vehicle_flow WHERE recorded_at >= NOW() - INTERVAL 5 MINUTE GROUP BY zone_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(flowSql)) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("zone_id"), k -> new java.util.HashMap<>())
                      .put("flowRate", rs.getDouble("avg_flow"));
            }
        } catch (SQLException e) { log.error("Zone flow query failed: {}", e.getMessage()); }

        // Latest avg CO2 per zone (last 5 minutes)
        String co2Sql = "SELECT zone_id, AVG(co2_level) AS avg_co2 " +
                        "FROM pollution_data WHERE recorded_at >= NOW() - INTERVAL 5 MINUTE GROUP BY zone_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(co2Sql)) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("zone_id"), k -> new java.util.HashMap<>())
                      .put("co2", rs.getDouble("avg_co2"));
            }
        } catch (SQLException e) { log.error("Zone co2 query failed: {}", e.getMessage()); }

        // Active alert count per zone
        String alertSql = "SELECT zone_id, COUNT(*) AS alert_count FROM alerts WHERE is_active = TRUE GROUP BY zone_id";
        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(alertSql)) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("zone_id"), k -> new java.util.HashMap<>())
                      .put("alertCount", rs.getInt("alert_count"));
            }
        } catch (SQLException e) { log.error("Zone alert count query failed: {}", e.getMessage()); }

        return result;
    }
}
