package com.shakilpatel.org.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;
import java.util.logging.Logger;

@Singleton
//@Startup
public class KafkaProducerBean {

    private static final Logger LOGGER = Logger.getLogger(KafkaProducerBean.class.getName());

    @ConfigProperty(name = "kafka.bootstrap.servers")
    private String bootstrapServers;

    private KafkaProducer<String, String> producer;
    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        bootstrapServers = ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);

        producer = new KafkaProducer<>(props);
        jsonb = JsonbBuilder.create();

        LOGGER.info("Kafka Producer initialized with bootstrap servers: " + bootstrapServers);
    }

    public void sendEvent(String topic, Object event) {
        try {
            String json = jsonb.toJson(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("Failed to send event to topic " + topic + ": " + exception.getMessage());
                } else {
                    LOGGER.info("Event sent to topic " + topic + " at offset " + metadata.offset());
                }
            });
        } catch (Exception e) {
            LOGGER.severe("Error serializing event: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            producer.close();
            LOGGER.info("Kafka Producer closed");
        }
        if (jsonb != null) {
            try {
                jsonb.close();
            } catch (Exception e) {
                LOGGER.warning("Error closing Jsonb: " + e.getMessage());
            }
        }
    }
}
