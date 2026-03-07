package com.iutms.feux.kafka;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iutms.common.kafka.KafkaConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Kafka consumer for the {@code traffic-alerts} topic.
 * Subscribes with consumer group {@code feux-alert-reactor} and delegates
 * each alert to {@link AlertReactor} for signal adjustment.
 *
 * <p>Commits offsets manually after each successful record (at-least-once delivery).
 * Designed to run in a dedicated daemon thread started by {@link com.iutms.feux.server.AlertConsumerListener}.
 */
public class AlertConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private static final String TOPIC = "traffic-alerts";

    private final AlertReactor reactor;
    private volatile boolean running = true;

    public AlertConsumer(AlertReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void run() {
        KafkaConsumer<String, String> consumer = KafkaConsumerFactory.create("feux-alert-reactor");
        consumer.subscribe(List.of(TOPIC));
        log.info("AlertConsumer subscribed to topic: {}", TOPIC);

        while (running) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        JsonObject json = JsonParser.parseString(record.value()).getAsJsonObject();
                        String zoneId    = getString(json, "zoneId");
                        String alertType = getString(json, "alertType");
                        reactor.react(alertType, zoneId);

                        consumer.commitSync(Collections.singletonMap(
                                new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset() + 1)));
                    } catch (Exception e) {
                        log.warn("Failed to process alert record offset {}: {}", record.offset(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("AlertConsumer poll error: {}", e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        consumer.close();
        log.info("AlertConsumer stopped");
    }

    public void stop() {
        running = false;
    }

    private String getString(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsString() : "unknown";
    }
}
