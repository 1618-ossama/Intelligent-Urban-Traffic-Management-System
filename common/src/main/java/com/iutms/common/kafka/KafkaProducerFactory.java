package com.iutms.common.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Factory for creating pre-configured Kafka producers.
 * All services use this to ensure consistent producer configuration.
 */
public class KafkaProducerFactory {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerFactory.class);

    public static KafkaProducer<String, String> create() {
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            bootstrapServers = "localhost:9092";
        }

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        log.info("Creating Kafka producer with bootstrap servers: {}", bootstrapServers);
        return new KafkaProducer<>(props);
    }
}
