package com.iutms.engine.alert;

import com.google.gson.JsonObject;
import com.iutms.common.kafka.KafkaProducerFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Publishes generated alerts to the {@code traffic-alerts} Kafka topic.
 * Enables closed-loop control: service-feux (and other services) can subscribe
 * to this topic and react to alerts in real time.
 */
public class AlertPublisher implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);
    private static final String TOPIC = "traffic-alerts";

    private final KafkaProducer<String, String> producer;

    public AlertPublisher() {
        this.producer = KafkaProducerFactory.create();
        log.info("AlertPublisher initialized, publishing to topic: {}", TOPIC);
    }

    /**
     * Publish an alert event to {@code traffic-alerts}.
     *
     * @param zoneId             zone that generated the alert
     * @param alertType          e.g. CONGESTION, POLLUTION, ACCIDENT, NOISE
     * @param severity           e.g. HIGH, MEDIUM, CRITICAL
     * @param percentOfThreshold sensor reading as percentage of its alert threshold
     */
    public void publish(String zoneId, String alertType, String severity, double percentOfThreshold) {
        JsonObject payload = new JsonObject();
        payload.addProperty("zoneId", zoneId);
        payload.addProperty("alertType", alertType);
        payload.addProperty("severity", severity);
        payload.addProperty("percentOfThreshold", percentOfThreshold);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, zoneId, payload.toString());
        producer.send(record, (metadata, ex) -> {
            if (ex != null) {
                log.error("Failed to publish alert to {}: {}", TOPIC, ex.getMessage());
            } else {
                log.debug("Alert published: {} zone={} offset={}", alertType, zoneId, metadata.offset());
            }
        });
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
        log.info("AlertPublisher closed");
    }
}
