package com.iutms.feux.service;

import com.iutms.common.util.JsonUtil;
import com.iutms.common.util.TimeUtil;
import com.iutms.feux.kafka.FeuxKafkaProducer;
import com.iutms.feux.model.SignalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the JAX-RPC service for traffic signal management.
 * Uses Apache Axis 1.4 for deployment.
 */
public class FeuxSignalisationServiceImpl implements FeuxSignalisationService {

    private static final Logger log = LoggerFactory.getLogger(FeuxSignalisationServiceImpl.class);

    private final Map<String, SignalState> signalStates = new ConcurrentHashMap<>();
    private FeuxKafkaProducer kafka;

    // Axis instantiation - no-arg constructor required
    public FeuxSignalisationServiceImpl() {
        try {
            this.kafka = new FeuxKafkaProducer();
        } catch (Exception e) {
            log.warn("Kafka producer init failed (will retry): {}", e.getMessage());
        }
        // Initialize default signals
        String[] intersections = {"INT-01", "INT-02", "INT-03", "INT-04", "INT-05", "INT-06"};
        String[] colors = {"GREEN", "RED", "GREEN", "RED", "GREEN", "YELLOW"};
        for (int i = 0; i < intersections.length; i++) {
            signalStates.put(intersections[i],
                    new SignalState(intersections[i], "zone-center", colors[i], 30, TimeUtil.now()));
        }
        log.info("FeuxSignalisationService initialized with {} intersections", intersections.length);
    }

    @Override
    public String getSignalState(String intersectionId) {
        SignalState state = signalStates.getOrDefault(intersectionId,
                new SignalState(intersectionId, "zone-center", "RED", 30, TimeUtil.now()));
        log.info("RPC getSignalState({}): {}", intersectionId, state);
        return JsonUtil.toJson(state);
    }

    @Override
    public boolean setSignalDuration(String intersectionId, String color, int durationSec) {
        SignalState newState = new SignalState(intersectionId, "zone-center",
                color, durationSec, TimeUtil.now());
        signalStates.put(intersectionId, newState);
        if (kafka != null) kafka.send(newState);
        log.info("RPC setSignalDuration: {}", newState);
        return true;
    }

    @Override
    public boolean synchronizeSignals(String zoneId) {
        log.info("RPC synchronizeSignals for zone: {}", zoneId);
        signalStates.forEach((id, state) -> {
            SignalState sync = new SignalState(id, zoneId, "GREEN", 45, TimeUtil.now());
            signalStates.put(id, sync);
            if (kafka != null) kafka.send(sync);
        });
        return true;
    }

    @Override
    public String getAllSignalStates(String zoneId) {
        List<SignalState> states = new ArrayList<>(signalStates.values());
        return JsonUtil.toJson(states);
    }
}
