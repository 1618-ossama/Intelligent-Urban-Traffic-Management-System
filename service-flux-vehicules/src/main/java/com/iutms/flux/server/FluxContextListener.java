package com.iutms.flux.server;

import com.iutms.flux.service.FluxVehiculesServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener that publishes the JAX-WS (SOAP) vehicle flow endpoint
 * on port 8081 when the service is deployed, and starts the simulator thread.
 */
public class FluxContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(FluxContextListener.class);
    private static final String URL = "http://0.0.0.0:9091/flux";

    private Endpoint endpoint;
    private Thread simulatorThread;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FluxVehiculesServiceImpl service = new FluxVehiculesServiceImpl();

        endpoint = Endpoint.publish(URL, service);
        log.info("JAX-WS FluxVehicules SOAP endpoint published at {}", URL);

        simulatorThread = new Thread(() -> {
            String[] zones = {"zone-center", "zone-north", "zone-south",
                              "zone-east", "zone-west", "zone-industrial"};
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (String zone : zones) {
                        service.getAllFlowData(zone);
                    }
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "flux-simulator");
        simulatorThread.setDaemon(true);
        simulatorThread.start();
        log.info("Flux simulator started (publishing every 3s)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (simulatorThread != null) simulatorThread.interrupt();
        if (endpoint != null) endpoint.stop();
        log.info("Flux endpoint stopped");
    }
}
