package com.iutms.engine.consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iutms.common.kafka.KafkaConsumerFactory;
import com.iutms.engine.analysis.TrafficAnalyzer;
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
 * Kafka consumer for a specific set of sensor topics.
 * Commits offsets manually after each successful record to guarantee at-least-once delivery.
 * Each instance should subscribe to one topic and run in its own thread.
 */
public class CentralConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CentralConsumer.class);

    private final List<String> topics;
    private final TrafficAnalyzer analyzer;
    private final String groupId;
    private volatile boolean running = true;
    private volatile KafkaConsumer<String, String> kafkaConsumer;
    private volatile Thread consumerThread;

    public CentralConsumer(List<String> topics, TrafficAnalyzer analyzer, String groupId) {
        this.topics   = topics;
        this.analyzer = analyzer;
        this.groupId  = groupId;
    }

    @Override
    public void run() {
        consumerThread = Thread.currentThread();
        KafkaConsumer<String, String> consumer = KafkaConsumerFactory.create(groupId);
        this.kafkaConsumer = consumer;
        consumer.subscribe(topics);
        log.info("Consumer subscribed to topics: {}", topics);

        while (running) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        processRecord(record);
                        consumer.commitSync(Collections.singletonMap(
                                new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset() + 1)));
                    } catch (Exception e) {
                        log.warn("Failed to process record from {} offset {}: {}",
                                record.topic(), record.offset(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Consumer poll error: {}", e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        consumer.close();
        log.info("Consumer stopped for topics: {}", topics);
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        JsonObject json = JsonParser.parseString(record.value()).getAsJsonObject();
        String topic = record.topic();

        switch (topic) {
            case "traffic-flow":
                analyzer.analyzeFlow(
                        getStr(json, "roadId"),
                        getStr(json, "zoneId"),
                        json.has("vehicleCount") ? json.get("vehicleCount").getAsInt() : 0,
                        json.has("flowRate") ? json.get("flowRate").getAsDouble() : 0.0
                );
                break;

            case "pollution-data":
                analyzer.analyzePollution(
                        getStr(json, "zoneId"),
                        json.has("co2Level") ? json.get("co2Level").getAsDouble() : 0.0,
                        json.has("noxLevel") ? json.get("noxLevel").getAsDouble() : 0.0,
                        json.has("pm25Level") ? json.get("pm25Level").getAsDouble() : 0.0
                );
                break;

            case "camera-events":
                analyzer.analyzeCameraEvent(
                        getStr(json, "cameraId"),
                        getStr(json, "zoneId"),
                        getStr(json, "eventType"),
                        getStr(json, "severity"),
                        getStr(json, "description")
                );
                break;

            case "noise-data":
                analyzer.analyzeNoise(
                        getStr(json, "zoneId"),
                        json.has("decibelLevel") ? json.get("decibelLevel").getAsDouble() : 0.0
                );
                break;

            case "signal-events":
                analyzer.analyzeSignal(
                        getStr(json, "intersectionId"),
                        getStr(json, "zoneId"),
                        getStr(json, "color"),
                        json.has("durationSec") ? json.get("durationSec").getAsInt() : 30
                );
                break;

            default:
                log.warn("Unknown topic: {}", topic);
        }
    }

    private String getStr(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsString() : "unknown";
    }

    public void stop() {
        KafkaConsumer<String, String> c = this.kafkaConsumer;
        if (c != null) c.wakeup();
        running = false;
        Thread t = consumerThread;
        if (t != null) {
            try { t.join(2000); } catch (InterruptedException ignored) {}
        }
    }
}
