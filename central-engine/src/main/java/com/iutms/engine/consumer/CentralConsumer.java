package com.iutms.engine.consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iutms.common.kafka.KafkaConsumerFactory;
import com.iutms.engine.analysis.TrafficAnalyzer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Central Kafka consumer that subscribes to ALL sensor topics
 * and routes messages to the TrafficAnalyzer for processing.
 */
public class CentralConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CentralConsumer.class);

    private static final List<String> TOPICS = Arrays.asList(
            "traffic-flow", "pollution-data", "camera-events", "noise-data", "signal-events"
    );

    private final TrafficAnalyzer analyzer;
    private volatile boolean running = true;

    public CentralConsumer(TrafficAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public void run() {
        KafkaConsumer<String, String> consumer = KafkaConsumerFactory.create("iutms-central-engine");
        consumer.subscribe(TOPICS);
        log.info("Central consumer subscribed to topics: {}", TOPICS);

        while (running) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
            } catch (Exception e) {
                log.error("Consumer error: {}", e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        consumer.close();
        log.info("Central consumer stopped");
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
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
        } catch (Exception e) {
            log.error("Error processing record from {}: {}", record.topic(), e.getMessage());
        }
    }

    private String getStr(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsString() : "unknown";
    }

    public void stop() {
        running = false;
    }
}
