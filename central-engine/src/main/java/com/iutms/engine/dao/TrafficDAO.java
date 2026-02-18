package com.iutms.engine.dao;

import com.iutms.engine.config.DatabaseConfig;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for persisting sensor data, alerts, and recommendations.
 */
public class TrafficDAO {

    private static final Logger log = LoggerFactory.getLogger(TrafficDAO.class);

    public void insertVehicleFlow(String roadId, String zoneId, int count, double rate) {
        String sql = "INSERT INTO vehicle_flow (road_id, zone_id, vehicle_count, flow_rate) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roadId); ps.setString(2, zoneId);
            ps.setInt(3, count); ps.setDouble(4, rate);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert vehicle_flow failed: {}", e.getMessage()); }
    }

    public void insertPollution(String zoneId, double co2, double nox, double pm25) {
        String sql = "INSERT INTO pollution_data (zone_id, co2_level, nox_level, pm25_level) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId); ps.setDouble(2, co2);
            ps.setDouble(3, nox); ps.setDouble(4, pm25);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert pollution_data failed: {}", e.getMessage()); }
    }

    public void insertCameraEvent(String camId, String zoneId, String type, String severity, String desc) {
        String sql = "INSERT INTO camera_events (camera_id, zone_id, event_type, severity, description) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, camId); ps.setString(2, zoneId); ps.setString(3, type);
            ps.setString(4, severity); ps.setString(5, desc);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert camera_events failed: {}", e.getMessage()); }
    }

    public void insertNoise(String zoneId, double dB) {
        String sql = "INSERT INTO noise_data (zone_id, decibel_level) VALUES (?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, zoneId); ps.setDouble(2, dB);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert noise_data failed: {}", e.getMessage()); }
    }

    public void insertSignalState(String intId, String zoneId, String color, int duration) {
        String sql = "INSERT INTO signal_states (intersection_id, zone_id, color, duration_sec) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, intId); ps.setString(2, zoneId);
            ps.setString(3, color); ps.setInt(4, duration);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert signal_states failed: {}", e.getMessage()); }
    }

    public long insertAlert(Alert alert) {
        String sql = "INSERT INTO alerts (alert_type, zone_id, severity, message) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alert.getAlertType()); ps.setString(2, alert.getZoneId());
            ps.setString(3, alert.getSeverity()); ps.setString(4, alert.getMessage());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { log.error("Insert alert failed: {}", e.getMessage()); }
        return -1;
    }

    public void insertRecommendation(Recommendation rec) {
        String sql = "INSERT INTO recommendations (alert_id, action_type, description) VALUES (?,?,?)";
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, rec.getAlertId()); ps.setString(2, rec.getActionType());
            ps.setString(3, rec.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Insert recommendation failed: {}", e.getMessage()); }
    }

    public List<Alert> getActiveAlerts() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE is_active = TRUE ORDER BY created_at DESC LIMIT 50";
        try (Connection c = DatabaseConfig.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Alert a = new Alert();
                a.setId(rs.getLong("id")); a.setAlertType(rs.getString("alert_type"));
                a.setZoneId(rs.getString("zone_id")); a.setSeverity(rs.getString("severity"));
                a.setMessage(rs.getString("message")); a.setActive(rs.getBoolean("is_active"));
                a.setCreatedAt(rs.getString("created_at"));
                alerts.add(a);
            }
        } catch (SQLException e) { log.error("Query alerts failed: {}", e.getMessage()); }
        return alerts;
    }

    public List<Recommendation> getRecentRecommendations() {
        List<Recommendation> recs = new ArrayList<>();
        String sql = "SELECT * FROM recommendations ORDER BY created_at DESC LIMIT 50";
        try (Connection c = DatabaseConfig.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
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
}
