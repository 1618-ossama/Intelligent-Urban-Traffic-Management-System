package com.iutms.engine.registry;

import com.iutms.common.discovery.SensorRegistrationRequest;
import com.iutms.common.discovery.ServiceEndpointDescriptor;
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
class SensorRegistryTest {

    @Mock DataSource dataSource;
    @Mock Connection conn;
    @Mock PreparedStatement ps;
    @Mock Statement stmt;
    @Mock ResultSet rs;
    @Mock ZoneRegistry zoneRegistry;
    @Mock ServiceEndpointResolver resolver;

    private SensorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SensorRegistry(dataSource, zoneRegistry, resolver);
    }

    @Test
    void register_insertsRowAndReturnsDescriptor() throws SQLException {
        when(zoneRegistry.hasZone("zone-center")).thenReturn(true);
        ServiceEndpointDescriptor.EndpointInfo epInfo =
                new ServiceEndpointDescriptor.EndpointInfo("noise-svc", 9090, "TCP", "noise-data", 0);
        when(resolver.resolveForSensor("NOISE", "zone-center")).thenReturn(epInfo);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        SensorRegistrationRequest req = new SensorRegistrationRequest();
        req.setSensorId("sensor-001");
        req.setSensorType("NOISE");
        req.setZoneId("zone-center");

        ServiceEndpointDescriptor result = registry.register(req);

        assertNotNull(result);
        assertEquals("sensor-001", result.getSensorId());
        assertEquals("zone-center", result.getAssignedZone());
        verify(ps).executeUpdate();
    }

    @Test
    void register_sameIdTwice_doesNotThrow() throws SQLException {
        // ON DUPLICATE KEY UPDATE — executeUpdate returns 2 for update, no exception
        when(zoneRegistry.hasZone("zone-north")).thenReturn(true);
        ServiceEndpointDescriptor.EndpointInfo epInfo =
                new ServiceEndpointDescriptor.EndpointInfo("noise-svc", 9090, "TCP", "noise-data", 0);
        when(resolver.resolveForSensor("NOISE", "zone-north")).thenReturn(epInfo);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(2);

        SensorRegistrationRequest req = new SensorRegistrationRequest();
        req.setSensorId("sensor-001");
        req.setSensorType("NOISE");
        req.setZoneId("zone-north");

        assertDoesNotThrow(() -> registry.register(req));
        verify(ps, times(1)).executeUpdate();
    }

    @Test
    void heartbeat_updatesLastHeartbeat() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        registry.heartbeat("sensor-001");

        verify(conn).prepareStatement(contains("last_heartbeat"));
        verify(ps).setString(1, "sensor-001");
        verify(ps).executeUpdate();
    }

    @Test
    void markStale_returnsListOfStaleIds_andDoesNotPublishAlerts() throws SQLException {
        // markStale() opens two separate Connection blocks (select, then update)
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false); // 2 stale rows
        when(rs.getString("id")).thenReturn("sensor-A", "sensor-B");
        when(rs.getString("zone_id")).thenReturn("zone-center", "zone-north");
        when(stmt.executeUpdate(anyString())).thenReturn(2);

        List<SensorRegistry.StaleSensor> result = registry.markStale();

        assertEquals(2, result.size());
        assertEquals("sensor-A", result.get(0).sensorId());
        assertEquals("sensor-B", result.get(1).sensorId());
        // markStale itself must NOT touch zone data
        verifyNoInteractions(zoneRegistry);
    }
}
