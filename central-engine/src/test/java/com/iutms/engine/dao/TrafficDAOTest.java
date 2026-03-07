package com.iutms.engine.dao;

import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrafficDAOTest {

    @Mock DataSource dataSource;
    @Mock Connection conn;
    @Mock PreparedStatement ps;
    @Mock Statement stmt;
    @Mock ResultSet rs;

    private TrafficDAO dao;

    @BeforeEach
    void setUp() {
        dao = new TrafficDAO(dataSource);
    }

    // ── insertVehicleFlow ───────────────────────────────────────────────

    @Test
    void insertVehicleFlow_happyPath_callsExecuteUpdate() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        dao.insertVehicleFlow("road-1", "zone-center", 42, 1.5);

        verify(ps).executeUpdate();
    }

    @Test
    void insertVehicleFlow_sqlException_doesNotPropagate() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        assertDoesNotThrow(() -> dao.insertVehicleFlow("road-1", "zone-center", 42, 1.5));
    }

    // ── insertNoise ─────────────────────────────────────────────────────

    @Test
    void insertNoise_happyPath_callsExecuteUpdate() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        dao.insertNoise("zone-center", 87.5);

        verify(ps).executeUpdate();
    }

    @Test
    void insertNoise_sqlException_doesNotPropagate() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        assertDoesNotThrow(() -> dao.insertNoise("zone-center", 87.5));
    }

    // ── insertAlert ─────────────────────────────────────────────────────

    @Test
    void insertAlert_happyPath_returnsGeneratedKey() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(42L);

        Alert alert = new Alert("NOISE", "zone-center", "HIGH", "Noise above threshold");
        long id = dao.insertAlert(alert);

        assertEquals(42L, id);
    }

    @Test
    void insertAlert_sqlException_returnsMinusOne() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        Alert alert = new Alert("NOISE", "zone-center", "HIGH", "Noise above threshold");
        long id = dao.insertAlert(alert);

        assertEquals(-1L, id);
    }

    // ── getActiveAlerts ─────────────────────────────────────────────────

    @Test
    void getActiveAlerts_emptyResultSet_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Alert> alerts = dao.getActiveAlerts();

        assertTrue(alerts.isEmpty());
    }

    @Test
    void getActiveAlerts_oneRow_returnsPopulatedAlert() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("alert_type")).thenReturn("CONGESTION");
        when(rs.getString("zone_id")).thenReturn("zone-north");
        when(rs.getString("severity")).thenReturn("MEDIUM");
        when(rs.getString("message")).thenReturn("Traffic jam detected");
        when(rs.getBoolean("is_active")).thenReturn(true);
        when(rs.getString("created_at")).thenReturn("2026-02-28T10:00:00");

        List<Alert> alerts = dao.getActiveAlerts();

        assertEquals(1, alerts.size());
        Alert a = alerts.get(0);
        assertEquals(7L, a.getId());
        assertEquals("CONGESTION", a.getAlertType());
        assertEquals("zone-north", a.getZoneId());
        assertEquals("MEDIUM", a.getSeverity());
        assertEquals("Traffic jam detected", a.getMessage());
        assertTrue(a.isActive());
        assertEquals("2026-02-28T10:00:00", a.getCreatedAt());
    }

    @Test
    void getActiveAlerts_sqlException_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        List<Alert> alerts = dao.getActiveAlerts();

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    // ── getRecentRecommendations ────────────────────────────────────────

    @Test
    void getRecentRecommendations_emptyResultSet_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Recommendation> recs = dao.getRecentRecommendations();

        assertTrue(recs.isEmpty());
    }

    @Test
    void getRecentRecommendations_oneRow_returnsPopulatedRecommendation() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("id")).thenReturn(3L);
        when(rs.getLong("alert_id")).thenReturn(7L);
        when(rs.getString("action_type")).thenReturn("EXTEND_GREEN");
        when(rs.getString("description")).thenReturn("Extend green phase by 30s");
        when(rs.getString("status")).thenReturn("PENDING");
        when(rs.getString("created_at")).thenReturn("2026-02-28T10:01:00");

        List<Recommendation> recs = dao.getRecentRecommendations();

        assertEquals(1, recs.size());
        Recommendation r = recs.get(0);
        assertEquals(3L, r.getId());
        assertEquals(7L, r.getAlertId());
        assertEquals("EXTEND_GREEN", r.getActionType());
        assertEquals("Extend green phase by 30s", r.getDescription());
        assertEquals("PENDING", r.getStatus());
        assertEquals("2026-02-28T10:01:00", r.getCreatedAt());
    }

    @Test
    void getRecentRecommendations_sqlException_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        List<Recommendation> recs = dao.getRecentRecommendations();

        assertNotNull(recs);
        assertTrue(recs.isEmpty());
    }
}
