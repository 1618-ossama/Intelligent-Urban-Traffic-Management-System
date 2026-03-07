package com.iutms.feux.kafka;

import com.iutms.common.config.GeographyConfig;
import com.iutms.feux.service.FeuxSignalisationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Reacts to alerts from the central engine by adjusting traffic signal durations.
 *
 * <p>Zone → intersection mapping is loaded dynamically from GeographyConfig instead of hardcoded.
 */
public class AlertReactor {

    private static final Logger log = LoggerFactory.getLogger(AlertReactor.class);

    private final FeuxSignalisationServiceImpl service;
    private final GeographyConfig geography;

    public AlertReactor(FeuxSignalisationServiceImpl service) {
        this.service = service;
        this.geography = GeographyConfig.getInstance();
    }

    /**
     * React to an alert by adjusting signals for all intersections in the affected zone.
     *
     * @param alertType e.g. CONGESTION, ACCIDENT, NOISE, POLLUTION
     * @param zoneId    affected zone
     */
    public void react(String alertType, String zoneId) {
        List<String> intersections = geography.getIntersectionsForZone(zoneId);
        if (intersections.isEmpty()) {
            log.warn("AlertReactor: no intersections mapped for zone '{}', skipping reaction", zoneId);
            return;
        }

        switch (alertType) {
            case "CONGESTION":
                // Extend GREEN to 60 s to ease traffic flow
                for (String id : intersections) {
                    service.setSignalDuration(id, "GREEN", 60);
                    log.info("signal adjusted: {} {} GREEN 60s (CONGESTION in {})", id, zoneId, zoneId);
                }
                break;

            case "ACCIDENT":
                // Set all zone signals to RED for 120 s to clear the area
                for (String id : intersections) {
                    service.setSignalDuration(id, "RED", 120);
                    log.info("signal adjusted: {} {} RED 120s (ACCIDENT in {})", id, zoneId, zoneId);
                }
                break;

            case "NOISE":
            case "POLLUTION":
                // Extend RED to 90 s to slow traffic and reduce emissions/noise
                for (String id : intersections) {
                    service.setSignalDuration(id, "RED", 90);
                    log.info("signal adjusted: {} {} RED 90s ({} in {})", id, zoneId, alertType, zoneId);
                }
                break;

            default:
                log.debug("AlertReactor: no reaction defined for alertType '{}'", alertType);
        }
    }
}
