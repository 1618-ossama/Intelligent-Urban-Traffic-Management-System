package com.iutms.flux.service;

import com.iutms.flux.kafka.FluxKafkaProducer;
import com.iutms.flux.model.VehicleFlowData;
import com.iutms.flux.simulator.FluxSimulator;
import jakarta.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the JAX-WS SOAP service for vehicle flow data.
 * Generates simulated data, publishes to Kafka, and returns SOAP responses.
 */
@WebService(endpointInterface = "com.iutms.flux.service.FluxVehiculesService")
public class FluxVehiculesServiceImpl implements FluxVehiculesService {

    private static final Logger log = LoggerFactory.getLogger(FluxVehiculesServiceImpl.class);
    private final FluxKafkaProducer kafkaProducer;
    private final FluxSimulator simulator;

    public FluxVehiculesServiceImpl() {
        this.kafkaProducer = new FluxKafkaProducer();
        this.simulator = new FluxSimulator();
        log.info("FluxVehiculesService initialized");
    }

    @Override
    public VehicleFlowData getVehicleCount(String roadId) {
        VehicleFlowData data = simulator.generateFlowData(roadId, "zone-center");
        kafkaProducer.send(data);
        log.info("SOAP getVehicleCount: {}", data);
        return data;
    }

    @Override
    public double getFlowRate(String roadId) {
        VehicleFlowData data = simulator.generateFlowData(roadId, "zone-center");
        kafkaProducer.send(data);
        return data.getFlowRate();
    }

    @Override
    public List<VehicleFlowData> getAllFlowData(String zoneId) {
        List<VehicleFlowData> dataList = simulator.generateZoneData(zoneId);
        dataList.forEach(kafkaProducer::send);
        log.info("SOAP getAllFlowData: {} records for zone {}", dataList.size(), zoneId);
        return dataList;
    }
}
