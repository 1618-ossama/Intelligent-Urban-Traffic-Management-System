package com.iutms.engine.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    public static void init() {
        String host = env("MYSQL_HOST", "localhost");
        String port = env("MYSQL_PORT", "3306");
        String db = env("MYSQL_DB", "iutms_db");
        String user = env("MYSQL_USER", "iutms_user");
        String pass = env("MYSQL_PASS", "iutms_pass");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized: {}:{}/{}", host, port, db);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null && !val.isEmpty() ? val : defaultValue;
    }
}
