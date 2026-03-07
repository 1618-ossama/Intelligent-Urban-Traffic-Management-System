package com.iutms.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Runtime geography configuration loaded from database.
 * Provides zones, intersections, roads, and cameras to all services
 * without hardcoding city layout.
 * 
 * Singleton pattern with lazy loading on first access.
 */
public class GeographyConfig {
    private static final Logger log = LoggerFactory.getLogger(GeographyConfig.class);
    
    private static volatile GeographyConfig instance;
    private static final Object lock = new Object();
    
    // Data structures
    private List<String> zones = new ArrayList<>();
    private List<String> roads = new ArrayList<>();
    private List<String> intersections = new ArrayList<>();
    private List<String> cameras = new ArrayList<>();
    private Map<String, List<String>> zoneIntersectionMap = new HashMap<>();
    
    // Fallback defaults (for backward compatibility)
    private static final List<String> DEFAULT_ZONES = 
        List.of("zone-center", "zone-north", "zone-south", "zone-east", "zone-west", "zone-industrial");
    private static final List<String> DEFAULT_ROADS = 
        List.of("road-A", "road-B", "road-C", "road-D", "road-E");
    private static final List<String> DEFAULT_INTERSECTIONS = 
        List.of("INT-01", "INT-02", "INT-03", "INT-04", "INT-05", "INT-06");
    private static final List<String> DEFAULT_CAMERAS = 
        List.of("cam-01", "cam-02", "cam-03", "cam-04", "cam-05", "cam-06");

    private GeographyConfig() {}

    /**
     * Get or create singleton instance.
     * Uses fallback defaults if not explicitly loaded from DB.
     */
    public static GeographyConfig getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new GeographyConfig();
                    instance.loadDefaults();
                    log.info("GeographyConfig initialized with {} zones, {} roads, {} intersections, {} cameras",
                            instance.zones.size(), instance.roads.size(), 
                            instance.intersections.size(), instance.cameras.size());
                }
            }
        }
        return instance;
    }

    /**
     * Initialize with data from database or external source.
     * Call this during application startup after DB is ready.
     */
    public static void initialize(List<String> loadedZones, List<String> loadedRoads,
                                  List<String> loadedIntersections, List<String> loadedCameras,
                                  Map<String, List<String>> zoneToIntersections) {
        synchronized (lock) {
            instance = new GeographyConfig();
            instance.zones = new ArrayList<>(loadedZones != null ? loadedZones : DEFAULT_ZONES);
            instance.roads = new ArrayList<>(loadedRoads != null ? loadedRoads : DEFAULT_ROADS);
            instance.intersections = new ArrayList<>(loadedIntersections != null ? loadedIntersections : DEFAULT_INTERSECTIONS);
            instance.cameras = new ArrayList<>(loadedCameras != null ? loadedCameras : DEFAULT_CAMERAS);
            instance.zoneIntersectionMap = new HashMap<>(zoneToIntersections != null ? zoneToIntersections : new HashMap<>());
            log.info("GeographyConfig loaded: {} zones, {} roads, {} intersections, {} cameras",
                    instance.zones.size(), instance.roads.size(),
                    instance.intersections.size(), instance.cameras.size());
        }
    }

    /**
     * Load default hardcoded values (backward compatibility).
     */
    private void loadDefaults() {
        this.zones = new ArrayList<>(DEFAULT_ZONES);
        this.roads = new ArrayList<>(DEFAULT_ROADS);
        this.intersections = new ArrayList<>(DEFAULT_INTERSECTIONS);
        this.cameras = new ArrayList<>(DEFAULT_CAMERAS);
        
        // Default zone-intersection mappings
        this.zoneIntersectionMap.put("zone-center", List.of("INT-01", "INT-02"));
        this.zoneIntersectionMap.put("zone-north", List.of("INT-03", "INT-04"));
        this.zoneIntersectionMap.put("zone-south", List.of("INT-05", "INT-06"));
        this.zoneIntersectionMap.put("zone-east", List.of("INT-01", "INT-03"));
        this.zoneIntersectionMap.put("zone-west", List.of("INT-02", "INT-05"));
        this.zoneIntersectionMap.put("zone-industrial", List.of("INT-04", "INT-06"));
    }

    // ── Getters ──
    
    public List<String> getZones() {
        return Collections.unmodifiableList(getInstance().zones);
    }

    public List<String> getRoads() {
        return Collections.unmodifiableList(getInstance().roads);
    }

    public List<String> getIntersections() {
        return Collections.unmodifiableList(getInstance().intersections);
    }

    public List<String> getCameras() {
        return Collections.unmodifiableList(getInstance().cameras);
    }

    /**
     * Get intersections for a specific zone.
     * Returns empty list if zone not mapped.
     */
    public List<String> getIntersectionsForZone(String zoneId) {
        return getInstance().zoneIntersectionMap.getOrDefault(zoneId, Collections.emptyList());
    }

    /**
     * Check if zone exists in configuration.
     */
    public boolean hasZone(String zoneId) {
        return getInstance().zones.contains(zoneId);
    }

    /**
     * Check if road exists in configuration.
     */
    public boolean hasRoad(String roadId) {
        return getInstance().roads.contains(roadId);
    }

    /**
     * Check if intersection exists in configuration.
     */
    public boolean hasIntersection(String intersectionId) {
        return getInstance().intersections.contains(intersectionId);
    }

    /**
     * Export as map for API serialization.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "zones", getInstance().zones,
            "roads", getInstance().roads,
            "intersections", getInstance().intersections,
            "cameras", getInstance().cameras,
            "zoneIntersectionMap", getInstance().zoneIntersectionMap
        );
    }

    /**
     * Reset to defaults (testing only).
     */
    public static void reset() {
        synchronized (lock) {
            instance = null;
        }
    }
}
