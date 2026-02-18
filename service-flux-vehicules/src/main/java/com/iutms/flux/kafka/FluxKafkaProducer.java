package com.iutms.flux.kafka;

import com.iutms.common.kafka.KafkaProducerFactory;
import com.iutms.common.util.JsonUtil;
import com.iutms.flux.model.VehicleFlowData;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes vehicle flow data to the "traffic-flow" Kafka topic.
 */
public class FluxKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(FluxKafkaProducer.class);
    private static final String TOPIC = "traffic-flow";
    private final KafkaProducer<String, String> producer;

    public FluxKafkaProducer() {
        this.producer = KafkaProducerFactory.create();
    }

    public void send(VehicleFlowData data) {
        String json = JsonUtil.toJson(data);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(TOPIC, data.getRoadId(), json);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send to Kafka: {}", exception.getMessage());
            } else {
                log.debug("Sent to {}: partition={}, offset={}",
                        TOPIC, metadata.partition(), metadata.offset());
            }
        });
    }

    public void close() {
        producer.close();
    }
}
