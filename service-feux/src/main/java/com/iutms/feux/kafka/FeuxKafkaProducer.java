package com.iutms.feux.kafka;

import com.iutms.common.kafka.KafkaProducerFactory;
import com.iutms.common.util.JsonUtil;
import com.iutms.feux.model.SignalState;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeuxKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(FeuxKafkaProducer.class);
    private static final String TOPIC = "signal-events";
    private final KafkaProducer<String, String> producer;

    public FeuxKafkaProducer() {
        this.producer = KafkaProducerFactory.create();
    }

    public void send(SignalState state) {
        String json = JsonUtil.toJson(state);
        producer.send(new ProducerRecord<>(TOPIC, state.getZoneId(), json),
                (m, ex) -> {
                    if (ex != null) log.error("Kafka send failed: {}", ex.getMessage());
                    else log.debug("Sent to {}: offset={}", TOPIC, m.offset());
                });
    }
}
