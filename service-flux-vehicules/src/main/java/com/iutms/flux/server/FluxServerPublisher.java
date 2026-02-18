package com.iutms.flux.server;

import com.iutms.flux.service.FluxVehiculesServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher for the JAX-WS SOAP service.
 * Publishes the endpoint and starts a simulator loop to periodically
 * generate and publish flow data to Kafka.
 */
public class FluxServerPublisher {

    private static final Logger log = LoggerFactory.getLogger(FluxServerPublisher.class);
    private static final String URL = "http://0.0.0.0:8081/flux";

    public static void main(String[] args) {
        FluxVehiculesServiceImpl service = new FluxVehiculesServiceImpl();

        // Publish SOAP endpoint
        Endpoint.publish(URL, service);
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  ServiceFluxVehicules (JAX-WS) is running   ║");
        log.info("║  Endpoint: {}                ║", URL);
        log.info("║  WSDL:     {}?wsdl           ║", URL);
        log.info("╚══════════════════════════════════════════════╝");

        // Start auto-simulator thread that publishes data every 3 seconds
        Thread simulatorThread = new Thread(() -> {
            String[] roads = {"road-A", "road-B", "road-C", "road-D"};
            while (true) {
                try {
                    for (String road : roads) {
                        service.getVehicleCount(road);
                    }
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "flux-simulator");
        simulatorThread.setDaemon(true);
        simulatorThread.start();
        log.info("Auto-simulator started (publishing every 3s)");
    }
}
