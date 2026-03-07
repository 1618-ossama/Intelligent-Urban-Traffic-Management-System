package com.iutms.feux.service;

/**
 * JAX-RPC Service interface for traffic signal control.
 * Deployed via Apache Axis 1.4.
 */
public interface FeuxSignalisationService {

    String getSignalState(String intersectionId);

    boolean setSignalDuration(String intersectionId, String color, int durationSec);

    boolean synchronizeSignals(String zoneId);

    String getAllSignalStates(String zoneId);
}
