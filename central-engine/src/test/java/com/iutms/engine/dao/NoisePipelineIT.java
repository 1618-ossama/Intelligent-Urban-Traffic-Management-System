package com.iutms.engine.dao;

import com.iutms.engine.analysis.TrafficAnalyzer;
import com.iutms.engine.config.ThresholdConfig;
import com.iutms.engine.model.Alert;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: verifies the end-to-end pipeline
 * Kafka message -> TrafficAnalyzer.analyzeNoise() -> TrafficDAO.insertAlert() -> MySQL DB
 *
 * Note: Requires Docker. Uses Testcontainers to start Kafka and MySQL containers.
 * CentralConsumer is bypassed intentionally: its KafkaConsumerFactory reads KAFKA_BOOTSTRAP_SERVERS
 * from env var which cannot be overridden from Java code. The inline consumer here drives the same
 * TrafficAnalyzer + TrafficDAO pipeline, fully testing the analysis-to-DB path.
 *
 * Satisfies requirement F2.4.
 */
@Testcontainers
class NoisePipelineIT {

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @Container
    static final MySQLContainer<?> mysql =
        new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("iutms_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("integration/init-schema.sql");

    static TrafficDAO dao;
    static TrafficAnalyzer analyzer;
    static KafkaProducer<String, String> producer;
    static HikariDataSource dataSource;

    @BeforeAll
    static void setUp() {
        // Wire real production objects against container-supplied addresses
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(mysql.getJdbcUrl());
        cfg.setUsername(mysql.getUsername());
        cfg.setPassword(mysql.getPassword());
        cfg.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(cfg);

        // Package-private constructor -- valid because this test is in com.iutms.engine.dao
        dao = new TrafficDAO(dataSource);
        analyzer = new TrafficAnalyzer(dao, ThresholdConfig.of(100.0, 400.0, 35.0, 85.0));

        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        producer = new KafkaProducer<>(p);
    }

    @AfterAll
    static void tearDown() {
        if (producer != null) producer.close();
        if (dataSource != null) dataSource.close();
    }

    @Test
    void noiseAboveThreshold_consumedAndAlertPersistedInDB() throws Exception {
        String zoneId = "zone-test";
        double decibelLevel = 90.0; // above 85 dB threshold
        String payload = String.format(
            "{\"zoneId\":\"%s\",\"decibelLevel\":%.1f,\"timestamp\":\"2026-03-01T10:00:00\"}",
            zoneId, decibelLevel);

        // 1. Publish message to Kafka -- synchronous send (wait for broker ack)
        producer.send(new ProducerRecord<>("noise-data", zoneId, payload)).get(10, TimeUnit.SECONDS);
        producer.flush();

        // 2. Inline consumer with earliest offset reset to guarantee seeing the message
        Properties cp = new Properties();
        cp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        cp.put(ConsumerConfig.GROUP_ID_CONFIG, "test-noise-pipeline");
        cp.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cp.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cp.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // CRITICAL: must see pre-subscribe messages
        cp.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cp)) {
            consumer.subscribe(Collections.singletonList("noise-data"));

            // 3. Drive the pipeline inline -- poll then call analyzer directly
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            for (ConsumerRecord<String, String> record : records) {
                JsonObject json = JsonParser.parseString(record.value()).getAsJsonObject();
                String z = json.has("zoneId") ? json.get("zoneId").getAsString() : "unknown";
                double dB = json.has("decibelLevel") ? json.get("decibelLevel").getAsDouble() : 0.0;
                // Drives: dao.insertNoise() + dao.insertAlert() if dB > 85
                analyzer.analyzeNoise(z, dB);
            }
        }

        // 4. Assert alert persisted -- use Awaitility to handle any async DB commit lag
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Alert> alerts = dao.getActiveAlerts();
                assertTrue(
                    alerts.stream().anyMatch(a ->
                        "NOISE".equals(a.getAlertType()) && zoneId.equals(a.getZoneId())),
                    "Expected NOISE alert for zone-test after 90 dB message, but found: " + alerts
                );
            });
    }
}
