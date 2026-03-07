package com.iutms.engine.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneRegistryTest {

    @Mock DataSource dataSource;
    @Mock Connection conn;
    @Mock PreparedStatement ps;
    @Mock Statement stmt;
    @Mock ResultSet rs;

    private ZoneRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ZoneRegistry(dataSource);
    }

    @Test
    void hasZone_returnsTrueForKnownZone() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("id")).thenReturn("zone-center");
        when(rs.getString("name")).thenReturn("Center");
        when(rs.getDouble("latitude")).thenReturn(33.5);
        when(rs.getDouble("longitude")).thenReturn(-7.6);
        when(rs.getInt("radius_m")).thenReturn(500);
        when(rs.getDouble("area_sq_km")).thenReturn(1.0);
        when(rs.getInt("partition_idx")).thenReturn(0);

        registry.reload();

        assertTrue(registry.hasZone("zone-center"));
        assertFalse(registry.hasZone("zone-unknown"));
    }

    @Test
    void getZone_returnsCorrectPartitionIdxFromDb() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("id")).thenReturn("zone-north");
        when(rs.getString("name")).thenReturn("North");
        when(rs.getDouble("latitude")).thenReturn(33.6);
        when(rs.getDouble("longitude")).thenReturn(-7.5);
        when(rs.getInt("radius_m")).thenReturn(600);
        when(rs.getDouble("area_sq_km")).thenReturn(1.2);
        when(rs.getInt("partition_idx")).thenReturn(2);

        registry.reload();

        ZoneRegistry.ZoneInfo zone = registry.getZone("zone-north");
        assertNotNull(zone);
        assertEquals(2, zone.partitionIdx());
    }

    @Test
    void crc32FallbackForUnknownZone_isDeterministic() {
        // Cache is empty — partitionFor must fall back to CRC32 deterministically
        int first  = registry.partitionFor("zone-unknown-xyz", 3);
        int second = registry.partitionFor("zone-unknown-xyz", 3);
        assertEquals(first, second);
        assertTrue(first >= 0 && first < 3);
    }

    @Test
    void updateZone_cacheIsConsistentImmediatelyAfterWrite() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        // reload() is called twice: once explicitly, once inside updateZone()
        when(rs.next()).thenReturn(true, false, true, false);
        when(rs.getString("id")).thenReturn("zone-center", "zone-center");
        when(rs.getString("name")).thenReturn("Center", "Center");
        when(rs.getDouble("latitude")).thenReturn(33.5, 33.5);
        when(rs.getDouble("longitude")).thenReturn(-7.6, -7.6);
        when(rs.getInt("radius_m")).thenReturn(500, 500);
        when(rs.getDouble("area_sq_km")).thenReturn(1.0, 1.0);
        when(rs.getInt("partition_idx")).thenReturn(0, 1); // 0 before update, 1 after

        registry.reload();
        assertEquals(0, registry.getZone("zone-center").partitionIdx());

        when(ps.executeUpdate()).thenReturn(1);
        registry.updateZone("zone-center", Map.of("partition_idx", 1));

        // Cache must reflect new value IMMEDIATELY (inline update before reload())
        assertEquals(1, registry.getZone("zone-center").partitionIdx());
    }
}
