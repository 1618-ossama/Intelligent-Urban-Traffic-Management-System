package com.iutms.pollution.kafka;

import com.iutms.common.kafka.KafkaProducerFactory;
import com.iutms.common.util.JsonUtil;
import com.iutms.pollution.model.PollutionData;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollutionKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PollutionKafkaProducer.class);
    private static final String TOPIC = "pollution-data";
    private final KafkaProducer<String, String> producer;

    public PollutionKafkaProducer() {
        this.producer = KafkaProducerFactory.create();
    }

    public void send(PollutionData data) {
        String json = JsonUtil.toJson(data);
        producer.send(new ProducerRecord<>(TOPIC, data.getZoneId(), json),
                (metadata, ex) -> {
                    if (ex != null) log.error("Kafka send failed: {}", ex.getMessage());
                    else log.debug("Sent to {}: offset={}", TOPIC, metadata.offset());
                });
    }
}
