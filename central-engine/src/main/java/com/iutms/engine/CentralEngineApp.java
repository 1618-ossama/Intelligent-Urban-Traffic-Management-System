package com.iutms.engine;

import com.iutms.engine.analysis.TrafficAnalyzer;
import com.iutms.engine.api.DashboardApiServer;
import com.iutms.engine.config.DatabaseConfig;
import com.iutms.engine.consumer.CentralConsumer;
import com.iutms.engine.dao.TrafficDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Central Analysis Engine.
 * Initializes database, starts Kafka consumers, and launches the REST API.
 */
public class CentralEngineApp {

    private static final Logger log = LoggerFactory.getLogger(CentralEngineApp.class);

    public static void main(String[] args) throws Exception {
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   IUTMS Central Analysis Engine Starting...      ║");
        log.info("╚══════════════════════════════════════════════════╝");

        // Wait for MySQL to be ready (Docker startup delay)
        waitForDatabase();

        // Initialize components
        DatabaseConfig.init();
        TrafficDAO dao = new TrafficDAO();
        TrafficAnalyzer analyzer = new TrafficAnalyzer(dao);

        // Start Kafka consumer thread
        CentralConsumer consumer = new CentralConsumer(analyzer);
        Thread consumerThread = new Thread(consumer, "central-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        // Start REST API server
        DashboardApiServer apiServer = new DashboardApiServer(dao);
        apiServer.start(8080);

        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   Central Engine fully operational!               ║");
        log.info("║   REST API: http://localhost:8080/api/dashboard   ║");
        log.info("║   Health:   http://localhost:8080/api/health      ║");
        log.info("║   Alerts:   http://localhost:8080/api/alerts      ║");
        log.info("╚══════════════════════════════════════════════════╝");

        // Keep main thread alive
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Central Engine...");
            consumer.stop();
        }));

        Thread.currentThread().join();
    }

    private static void waitForDatabase() {
        int maxRetries = 30;
        for (int i = 0; i < maxRetries; i++) {
            try {
                DatabaseConfig.init();
                DatabaseConfig.getConnection().close();
                log.info("Database connection established");
                return;
            } catch (Exception e) {
                log.info("Waiting for database... ({}/{})", i + 1, maxRetries);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        log.warn("Could not connect to database after {} attempts, continuing anyway", maxRetries);
    }
}
