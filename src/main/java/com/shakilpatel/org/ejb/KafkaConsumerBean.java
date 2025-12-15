package com.shakilpatel.org.ejb;

import com.shakilpatel.org.entity.Organization;
import com.shakilpatel.org.enums.ApprovalStatus;
import com.shakilpatel.org.enums.OrgType;
import com.shakilpatel.org.kafka.events.OrgCreatedEvent;
import com.shakilpatel.org.kafka.events.OrgRegistrationEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Singleton
@Startup
public class KafkaConsumerBean {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerBean.class.getName());

    @ConfigProperty(name = "kafka.bootstrap.servers")
    private String bootstrapServers;

    @ConfigProperty(name = "kafka.consumer.group.id")
    private String groupId;

    @ConfigProperty(name = "kafka.consumer.topic")
    private String topic;

    @ConfigProperty(name = "kafka.producer.topic.created")
    private String createdTopic;

    @Inject
    private OrgBean orgBean;

    @Inject
    private KafkaProducerBean producerBean;

    private KafkaConsumer<String, String> consumer;
    private Jsonb jsonb;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        bootstrapServers = ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class);
        groupId = ConfigProvider.getConfig().getValue("kafka.consumer.group.id", String.class);
        topic = ConfigProvider.getConfig().getValue("kafka.consumer.topic", String.class);
        createdTopic = ConfigProvider.getConfig().getValue("kafka.producer.topic.created", String.class);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        jsonb = JsonbBuilder.create();

        running.set(true);

        LOGGER.info("Kafka Consumer initialized for topic: " + topic);
    }

    @Schedule(hour = "*", minute = "*", second = "*/5", persistent = false)
    public void pollMessages() {
        if (!running.get()) {
            return;
        }

        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                LOGGER.info("Received message from topic " + record.topic() +
                        " at offset " + record.offset());
                processMessage(record.value());
            }
        } catch (Exception e) {
            LOGGER.severe("Error polling Kafka messages: " + e.getMessage());
        }
    }

    private void processMessage(String message) {
        try {
            OrgRegistrationEvent event = jsonb.fromJson(message, OrgRegistrationEvent.class);
            LOGGER.info("Processing org registration for user: " + event.getUserId());

            // Check if org already exists for this user
            if (orgBean.findByCreatedBy(event.getUserId()).isPresent()) {
                LOGGER.warning("Organization already exists for user: " + event.getUserId());
                return;
            }

            // Create organization
            Organization org = new Organization();
            org.setOrgName(event.getOrgData().getOrgName());
            org.setOrgEmail(event.getOrgData().getOrgEmail());
            org.setOrgType(event.getOrgData().getOrgType());
            org.setSector(event.getOrgData().getSector());
            org.setRegistrationNo(event.getOrgData().getRegistrationNo());
            org.setPhone(event.getOrgData().getPhone());
            org.setAddress(event.getOrgData().getAddress());
            org.setCity(event.getOrgData().getCity());
            org.setState(event.getOrgData().getState());
            org.setPincode(event.getOrgData().getPincode());
            org.setWebsite(event.getOrgData().getWebsite());
            org.setStatus(ApprovalStatus.PENDING);
//            org.setApprovedAt(LocalDateTime.now());
            org.setCreatedBy(event.getUserId());
            org.setIsActive(true);

            Organization created = orgBean.createOrganization(org);

            LOGGER.info("Organization created with ID: " + created.getId());

            // Publish org.created event
            OrgCreatedEvent createdEvent = new OrgCreatedEvent();
            createdEvent.setEventId(UUID.randomUUID().toString());
            createdEvent.setOrgId(created.getId());
            createdEvent.setUserId(event.getUserId());
            createdEvent.setOrgName(created.getOrgName());
            createdEvent.setStatus("PENDING");

            producerBean.sendEvent(createdTopic, createdEvent);

        } catch (Exception e) {
            LOGGER.severe("Error processing org registration event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        running.set(false);
        if (consumer != null) {
            consumer.close();
            LOGGER.info("Kafka Consumer closed");
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
