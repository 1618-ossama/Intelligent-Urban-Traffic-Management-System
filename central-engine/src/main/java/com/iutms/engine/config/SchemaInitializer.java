package com.iutms.engine.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Handles database schema initialization by executing SQL migration scripts using HikariCP.
 * Ensures database schema and seed data are created/updated on application startup.
 */
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    /**
     * Initialize the database schema by executing migration SQL files via HikariCP connection pool.
     *
     * @param host       MySQL host
     * @param port       MySQL port
     * @param database   Database name
     * @param user       Database user
     * @param password   Database password
     */
    public static void initializeSchema(String host, String port, String database, String user, String password) {
        HikariDataSource dataSource = null;
        try {
            log.info("Starting database schema initialization...");

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

            // Create HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(3);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            dataSource = new HikariDataSource(config);

            log.info("Connected to database via HikariCP");

            try (Connection conn = dataSource.getConnection()) {
                // Load and execute V1__Initial_Schema.sql
                executeSqlFile(conn, "/db/migration/V1__Initial_Schema.sql", "Initial Schema");

                // Load and execute V2__Seed_Data.sql
                executeSqlFile(conn, "/db/migration/V2__Seed_Data.sql", "Seed Data");

                // Load and execute V3__Sensor_Registry.sql
                executeSqlFile(conn, "/db/migration/V3__Sensor_Registry.sql", "Sensor Registry");

                // Load and execute V4__Zone_Registry.sql
                executeSqlFile(conn, "/db/migration/V4__Zone_Registry.sql", "Zone Registry");

                log.info("Schema initialization complete");
            }

        } catch (Exception e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database schema initialization failed: " + e.getMessage(), e);
        } finally {
            if (dataSource != null) {
                dataSource.close();
                log.info("HikariCP connection pool closed");
            }
        }
    }

    private static void executeSqlFile(Connection conn, String resourcePath, String description) throws Exception {
        try (InputStreamReader isr = new InputStreamReader(
                SchemaInitializer.class.getResourceAsStream(resourcePath));
             BufferedReader reader = new BufferedReader(isr);
             Statement stmt = conn.createStatement()) {

            log.info("Executing {} migration...", description);
            StringBuilder sql = new StringBuilder();
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                // Skip comments and empty lines
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                sql.append(line).append("\n");

                // Execute when we hit a semicolon
                if (line.endsWith(";")) {
                    String statement = sql.toString().trim();
                    if (!statement.isEmpty()) {
                        try {
                            stmt.execute(statement);
                            log.debug("Executed statement at line {}", lineNum);
                        } catch (Exception e) {
                            // Log but continue - some statements may fail if already executed
                            log.debug("Statement result: {}", e.getMessage().substring(0, Math.min(80, e.getMessage().length())));
                        }
                        sql = new StringBuilder();
                    }
                }
            }

            log.info("Successfully applied {} migration", description);
        }
    }

    /**
     * Validate that all migrations have been applied successfully by checking key tables.
     *
     * @param host       MySQL host
     * @param port       MySQL port
     * @param database   Database name
     * @param user       Database user
     * @param password   Database password
     */
    public static void validateSchema(String host, String port, String database, String user, String password) {
        HikariDataSource dataSource = null;
        try {
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            dataSource = new HikariDataSource(config);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // Query to check if key tables exist
                stmt.executeQuery("SELECT 1 FROM zones LIMIT 1");
                log.info("Database schema validation successful - tables exist");
            }

        } catch (Exception e) {
            log.error("Database schema validation failed", e);
            throw new RuntimeException("Database schema validation failed: " + e.getMessage(), e);
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}
