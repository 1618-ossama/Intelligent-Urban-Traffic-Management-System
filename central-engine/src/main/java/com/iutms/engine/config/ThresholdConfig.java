package com.iutms.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads alert thresholds from an external properties file or classpath defaults.
 * Load priority:
 *  1. Path in env var {@code THRESHOLDS_CONFIG}
 *  2. Classpath {@code thresholds.properties}
 *
 * <p>Use {@link #of(double, double, double, double)} in unit tests to inject known values
 * without touching the filesystem.
 */
public class ThresholdConfig {

    private static final Logger log = LoggerFactory.getLogger(ThresholdConfig.class);

    private final double flow;
    private final double co2;
    private final double pm25;
    private final double noise;

    /** Production constructor: loads from env var path or classpath. */
    public ThresholdConfig() {
        Properties props = load();
        this.flow  = parseDouble(props, "threshold.flow",  100.0);
        this.co2   = parseDouble(props, "threshold.co2",   400.0);
        this.pm25  = parseDouble(props, "threshold.pm25",  35.0);
        this.noise = parseDouble(props, "threshold.noise", 85.0);
        log.info("Thresholds loaded — flow={} co2={} pm25={} noise={}", flow, co2, pm25, noise);
    }

    /** Direct-value constructor for testing. */
    private ThresholdConfig(double flow, double co2, double pm25, double noise) {
        this.flow  = flow;
        this.co2   = co2;
        this.pm25  = pm25;
        this.noise = noise;
    }

    /** Factory method for tests: inject explicit values without a properties file. */
    public static ThresholdConfig of(double flow, double co2, double pm25, double noise) {
        return new ThresholdConfig(flow, co2, pm25, noise);
    }

    public double getFlow()  { return flow; }
    public double getCo2()   { return co2; }
    public double getPm25()  { return pm25; }
    public double getNoise() { return noise; }

    private Properties load() {
        Properties props = new Properties();
        String envPath = System.getenv("THRESHOLDS_CONFIG");

        if (envPath != null && !envPath.isBlank()) {
            try (InputStream in = new FileInputStream(envPath)) {
                props.load(in);
                log.info("Loaded thresholds from env path: {}", envPath);
                return props;
            } catch (Exception e) {
                log.warn("Could not load thresholds from '{}', falling back to classpath: {}", envPath, e.getMessage());
            }
        }

        try (InputStream in = ThresholdConfig.class.getClassLoader()
                .getResourceAsStream("thresholds.properties")) {
            if (in != null) {
                props.load(in);
                log.info("Loaded thresholds from classpath thresholds.properties");
            } else {
                log.warn("thresholds.properties not found on classpath; using built-in defaults");
            }
        } catch (Exception e) {
            log.warn("Could not load classpath thresholds.properties: {}", e.getMessage());
        }

        return props;
    }

    private double parseDouble(Properties props, String key, double defaultVal) {
        String val = props.getProperty(key);
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid threshold value for '{}': '{}'; using default {}", key, val, defaultVal);
            return defaultVal;
        }
    }
}
