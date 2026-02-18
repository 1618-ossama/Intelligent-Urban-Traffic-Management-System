package com.iutms.pollution.config;

import com.iutms.pollution.resource.PollutionResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class PollutionServer {

    private static final Logger log = LoggerFactory.getLogger(PollutionServer.class);
    private static final String BASE_URI = "http://0.0.0.0:8082/api/";

    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig()
                .register(PollutionResource.class)
                .register(JacksonFeature.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  ServicePollution (JAX-RS) is running        ║");
        log.info("║  Base URL: {}              ║", BASE_URI);
        log.info("║  Try: GET /api/pollution/zone-center          ║");
        log.info("╚══════════════════════════════════════════════╝");

        // Auto-simulator: poll every zone every 4 seconds
        PollutionResource resource = new PollutionResource();
        Thread sim = new Thread(() -> {
            String[] zones = {"zone-center", "zone-north", "zone-south", "zone-east"};
            while (true) {
                try {
                    for (String z : zones) resource.getPollution(z);
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "pollution-simulator");
        sim.setDaemon(true);
        sim.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
    }
}
