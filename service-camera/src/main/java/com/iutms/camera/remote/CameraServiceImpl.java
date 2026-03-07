package com.iutms.camera.remote;

import com.iutms.camera.kafka.CameraKafkaProducer;
import com.iutms.camera.model.CameraEvent;
import com.iutms.camera.simulator.CameraSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class CameraServiceImpl extends UnicastRemoteObject implements CameraService {

    private static final Logger log = LoggerFactory.getLogger(CameraServiceImpl.class);
    private final CameraSimulator simulator;
    private final CameraKafkaProducer kafka;

    public CameraServiceImpl() throws RemoteException {
        super();
        this.simulator = new CameraSimulator();
        this.kafka = new CameraKafkaProducer();
        log.info("CameraServiceImpl initialized");
    }

    @Override
    public CameraEvent getCameraStatus(String cameraId) throws RemoteException {
        CameraEvent event = simulator.getStatus(cameraId, "zone-center");
        kafka.send(event);
        log.info("RMI getCameraStatus: {}", event);
        return event;
    }

    @Override
    public boolean detectIncident(String cameraId) throws RemoteException {
        CameraEvent event = simulator.detectIncident(cameraId, "zone-center");
        if (event.isIncident()) {
            kafka.send(event);
            log.warn("INCIDENT detected by {}: {}", cameraId, event);
        }
        return event.isIncident();
    }

    @Override
    public List<CameraEvent> getZoneEvents(String zoneId) throws RemoteException {
        List<CameraEvent> events = simulator.getZoneEvents(zoneId);
        events.forEach(kafka::send);
        return events;
    }
}
