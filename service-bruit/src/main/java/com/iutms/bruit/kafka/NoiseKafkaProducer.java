package com.iutms.bruit.kafka;

import com.iutms.bruit.model.NoiseData;
import com.iutms.common.kafka.KafkaProducerFactory;
import com.iutms.common.util.JsonUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoiseKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(NoiseKafkaProducer.class);
    private static final String TOPIC = "noise-data";
    private final KafkaProducer<String, String> producer;

    public NoiseKafkaProducer() {
        this.producer = KafkaProducerFactory.create();
    }

    public void send(NoiseData data) {
        String json = JsonUtil.toJson(data);
        producer.send(new ProducerRecord<>(TOPIC, data.getZoneId(), json),
                (m, ex) -> {
                    if (ex != null) log.error("Kafka send failed: {}", ex.getMessage());
                    else log.debug("Sent to {}: offset={}", TOPIC, m.offset());
                });
    }
}
