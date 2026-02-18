package com.iutms.camera.kafka;

import com.iutms.camera.model.CameraEvent;
import com.iutms.common.kafka.KafkaProducerFactory;
import com.iutms.common.util.JsonUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(CameraKafkaProducer.class);
    private static final String TOPIC = "camera-events";
    private final KafkaProducer<String, String> producer;

    public CameraKafkaProducer() {
        this.producer = KafkaProducerFactory.create();
    }

    public void send(CameraEvent event) {
        String json = JsonUtil.toJson(event);
        producer.send(new ProducerRecord<>(TOPIC, event.getCameraId(), json),
                (m, ex) -> {
                    if (ex != null) log.error("Kafka send failed: {}", ex.getMessage());
                    else log.debug("Sent to {}: offset={}", TOPIC, m.offset());
                });
    }
}
