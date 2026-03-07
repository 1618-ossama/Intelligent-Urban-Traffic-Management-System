package com.iutms.engine;

import com.iutms.engine.alert.AlertPublisher;
import com.iutms.engine.analysis.TrafficAnalyzer;
import com.iutms.engine.api.DashboardApiServer;
import com.iutms.engine.config.DatabaseConfig;
import com.iutms.engine.config.SchemaInitializer;
import com.iutms.engine.config.ThresholdConfig;
import com.iutms.engine.consumer.CentralConsumer;
import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.registry.SensorRegistry;
import com.iutms.engine.registry.ServiceEndpointResolver;
import com.iutms.engine.registry.ZoneRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

        waitForDatabase();
        initializeSchema();

        // ── Core components ──────────────────────────────────────────────
        DatabaseConfig.init();
        TrafficDAO      dao       = new TrafficDAO();
        ThresholdConfig thresholds = new ThresholdConfig();
        AlertPublisher  publisher  = new AlertPublisher();
        TrafficAnalyzer analyzer   = new TrafficAnalyzer(dao, thresholds, publisher);

        // ── Registry layer (pass null DataSource → falls back to DatabaseConfig) ─
        ZoneRegistry            zoneRegistry   = new ZoneRegistry(null);
        zoneRegistry.reload();

        ServiceEndpointResolver resolver       = new ServiceEndpointResolver(zoneRegistry);
        SensorRegistry          sensorRegistry = new SensorRegistry(null, zoneRegistry, resolver);

        // ── Kafka consumers ──────────────────────────────────────────────
        String groupId = env("CONSUMER_GROUP_ID", "central-engine-group");
        String[] topics = {"traffic-flow", "pollution-data", "camera-events", "noise-data", "signal-events"};
        CentralConsumer[] consumers = new CentralConsumer[topics.length];
        for (int i = 0; i < topics.length; i++) {
            consumers[i] = new CentralConsumer(List.of(topics[i]), analyzer, groupId);
            Thread t = new Thread(consumers[i], "consumer-" + topics[i]);
            t.setDaemon(true);
            t.start();
        }

        // ── REST API ─────────────────────────────────────────────────────
        DashboardApiServer apiServer =
                new DashboardApiServer(dao, sensorRegistry, zoneRegistry);
        apiServer.start(8080);

        // ── Background tasks ─────────────────────────────────────────────
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Zone registry refresh every 5 minutes
        scheduler.scheduleAtFixedRate(zoneRegistry::reload, 5, 5, TimeUnit.MINUTES);

        // Stale sensor detection every 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<SensorRegistry.StaleSensor> stale = sensorRegistry.markStale();
                stale.forEach(s -> analyzer.raiseSensorStale(s.sensorId(), s.zoneId()));
            } catch (Exception e) {
                log.warn("markStale task error: {}", e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);

        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   Central Engine fully operational!               ║");
        log.info("║   REST API: http://localhost:8080/api/dashboard   ║");
        log.info("║   Health:   http://localhost:8080/api/health      ║");
        log.info("║   Alerts:   http://localhost:8080/api/alerts      ║");
        log.info("║   Sensors:  http://localhost:8080/api/sensors     ║");
        log.info("║   Zones:    http://localhost:8080/api/zones/config║");
        log.info("╚══════════════════════════════════════════════════╝");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Central Engine...");
            scheduler.shutdownNow();
            for (CentralConsumer c : consumers) c.stop();
            publisher.close();
        }));

        Thread.currentThread().join();
    }

    private static void waitForDatabase() {
        String host = env("MYSQL_HOST", "localhost");
        String port = env("MYSQL_PORT", "3306");
        String db   = env("MYSQL_DB",   "iutms_db");
        String user = env("MYSQL_USER", "iutms_user");
        String pass = env("MYSQL_PASS", "iutms_pass");
        String url  = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        int maxRetries = 30;
        for (int i = 0; i < maxRetries; i++) {
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url, user, pass)) {
                log.info("Database connection established");
                return;
            } catch (Exception e) {
                log.info("Waiting for database... ({}/{})", i + 1, maxRetries);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        log.warn("Could not connect to database after {} attempts, continuing anyway", maxRetries);
    }

    private static void initializeSchema() {
        try {
            String host     = env("MYSQL_HOST", "localhost");
            String port     = env("MYSQL_PORT", "3306");
            String database = env("MYSQL_DB",   "iutms_db");
            String user     = env("MYSQL_USER",  "iutms_user");
            String password = env("MYSQL_PASS",  "iutms_pass");

            log.info("Initializing database schema...");
            SchemaInitializer.initializeSchema(host, port, database, user, password);
        } catch (Exception e) {
            log.error("Failed to initialize schema", e);
            throw new RuntimeException(e);
        }
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null && !val.isEmpty() ? val : defaultValue;
    }
}
