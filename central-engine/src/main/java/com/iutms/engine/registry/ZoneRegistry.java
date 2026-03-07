package com.iutms.engine.registry;

import com.iutms.common.config.ZonePartitionConfig;
import com.iutms.engine.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DB-backed zone registry that replaces the static {@code GeographyConfig} singleton
 * for server-side zone validation and partition routing.
 *
 * <p>Zone data is loaded from the {@code zones} table at startup and refreshed
 * every 5 minutes by the {@code CentralEngineApp} scheduler, or immediately
 * after a zone is created/updated via the REST API.
 *
 * <p>Only {@code is_active = TRUE} zones are held in the in-memory cache.
 */
public class ZoneRegistry {

    private static final Logger log = LoggerFactory.getLogger(ZoneRegistry.class);

    private final DataSource dataSource;
    private volatile Map<String, ZoneInfo> cache = new ConcurrentHashMap<>();

    public ZoneRegistry(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Public Zone Info Record ───────────────────────────────────────────

    public record ZoneInfo(
            String id,
            String name,
            double lat,          // mapped from zones.latitude
            double lng,          // mapped from zones.longitude
            int    radiusM,
            double areaSqKm,
            int    partitionIdx
    ) {}

    // ── Query API ────────────────────────────────────────────────────────

    public boolean hasZone(String zoneId) {
        return cache.containsKey(zoneId);
    }

    public ZoneInfo getZone(String zoneId) {
        return cache.get(zoneId);
    }

    public List<ZoneInfo> getAllZones() {
        return new ArrayList<>(cache.values());
    }

    public List<String> getZonesByPartition(int partitionIdx) {
        List<String> result = new ArrayList<>();
        for (ZoneInfo z : cache.values()) {
            if (z.partitionIdx() == partitionIdx) result.add(z.id());
        }
        return result;
    }

    /**
     * Returns the Kafka partition index for the given zone.
     * Reads {@code partition_idx} from the cache; falls back to CRC32.
     */
    public int partitionFor(String zoneId, int numPartitions) {
        ZoneInfo z = cache.get(zoneId);
        if (z != null) return z.partitionIdx() % numPartitions;
        return ZonePartitionConfig.crc32Partition(zoneId, numPartitions);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * Load (or reload) active zones from the database into the in-memory cache.
     * Thread-safe: swaps the entire map atomically.
     */
    public void reload() {
        String sql = "SELECT id, name, latitude, longitude, area_sq_km, radius_m, partition_idx " +
                     "FROM zones WHERE is_active = TRUE";
        Map<String, ZoneInfo> fresh = new ConcurrentHashMap<>();
        try (Connection c = getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                ZoneInfo z = new ZoneInfo(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getInt("radius_m"),
                        rs.getDouble("area_sq_km"),
                        rs.getInt("partition_idx")
                );
                fresh.put(z.id(), z);
            }
            cache = fresh;
            log.info("ZoneRegistry loaded {} active zones", fresh.size());
        } catch (SQLException e) {
            log.error("ZoneRegistry reload failed: {}", e.getMessage());
        }
    }

    // ── Zone CRUD (for REST zone-config endpoints) ────────────────────────

    /**
     * Insert a new zone. Assigns CRC32 partition_idx if not provided (< 0).
     */
    public void createZone(String id, String name, double lat, double lng,
                           int radiusM, int partitionIdx) {
        int idx = (partitionIdx < 0)
                ? ZonePartitionConfig.crc32Partition(id, 3)
                : partitionIdx;
        String sql = "INSERT INTO zones (id, name, latitude, longitude, area_sq_km, " +
                     "radius_m, partition_idx, is_active) VALUES (?,?,?,?,0,?,?,TRUE)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setDouble(3, lat);
            ps.setDouble(4, lng);
            ps.setInt(5, radiusM);
            ps.setInt(6, idx);
            ps.executeUpdate();
            reload();
        } catch (SQLException e) {
            log.error("createZone failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Partially update a zone. Only non-null map values are applied.
     * Triggers an in-memory cache reload on success.
     */
    public void updateZone(String id, Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) return;
        StringBuilder sb = new StringBuilder("UPDATE zones SET ");
        List<Object> params = new ArrayList<>();
        updates.forEach((col, val) -> {
            if (!params.isEmpty()) sb.append(", ");
            sb.append(col).append(" = ?");
            params.add(val);
        });
        sb.append(" WHERE id = ?");
        params.add(id);
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ps.executeUpdate();
            // Inline cache update — immediate consistency without waiting for reload()
            ZoneInfo existing = cache.get(id);
            if (existing != null) {
                String name       = updates.containsKey("name")          ? (String) updates.get("name")           : existing.name();
                double lat        = updates.containsKey("latitude")       ? ((Number) updates.get("latitude")).doubleValue()  : existing.lat();
                double lng        = updates.containsKey("longitude")      ? ((Number) updates.get("longitude")).doubleValue() : existing.lng();
                int    radiusM    = updates.containsKey("radius_m")       ? ((Number) updates.get("radius_m")).intValue()     : existing.radiusM();
                int    partIdx    = updates.containsKey("partition_idx")  ? ((Number) updates.get("partition_idx")).intValue(): existing.partitionIdx();
                cache.put(id, new ZoneInfo(id, name, lat, lng, radiusM, existing.areaSqKm(), partIdx));
            }
            reload();
        } catch (SQLException e) {
            log.error("updateZone failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Soft-delete: marks the zone inactive and removes it from the cache.
     */
    public void deactivateZone(String id) {
        String sql = "UPDATE zones SET is_active = FALSE WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            cache.remove(id);
        } catch (SQLException e) {
            log.error("deactivateZone failed: {}", e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : DatabaseConfig.getConnection();
    }
}
