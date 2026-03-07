package com.iutms.common.config;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Deterministic zone-to-Kafka-partition mapping.
 *
 * <p>Primary: uses a pre-loaded {@code Map<zoneId, partitionIdx>} sourced from
 * the {@code zones} table (populated at startup by the Central Engine's ZoneRegistry
 * and passed to sensor services via configuration).
 *
 * <p>Fallback: CRC32 hash of the zoneId — deterministic across all JVMs and
 * platforms, unlike {@link String#hashCode()}.
 *
 * <p>Sensor services that lack DB access can instantiate with an empty map;
 * they will always use the CRC32 path, which is consistent as long as the
 * topic partition count does not change.
 */
public class ZonePartitionConfig {

    private final Map<String, Integer> partitionMap;

    public ZonePartitionConfig(Map<String, Integer> partitionMap) {
        this.partitionMap = partitionMap != null
                ? Collections.unmodifiableMap(new HashMap<>(partitionMap))
                : Collections.emptyMap();
    }

    /**
     * Return the Kafka partition index for the given zone.
     * Looks up the pre-loaded map first; falls back to CRC32.
     */
    public int partitionFor(String zoneId, int numPartitions) {
        Integer idx = partitionMap.get(zoneId);
        if (idx != null) return idx % numPartitions;
        return crc32Partition(zoneId, numPartitions);
    }

    /**
     * Stateless CRC32-based partition calculation.
     * Safe to call without a pre-loaded map.
     */
    public static int crc32Partition(String zoneId, int numPartitions) {
        CRC32 crc = new CRC32();
        crc.update(zoneId.getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % numPartitions);
    }
}
