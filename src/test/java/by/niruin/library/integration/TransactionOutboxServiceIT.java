package by.niruin.library.integration;

import by.niruin.library.kafka.KafkaProducer;
import by.niruin.library.model.event.EventType;
import by.niruin.library.repository.TransactionOutboxRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class TransactionOutboxServiceIT extends BaseIntegrationTest {
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private KafkaProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }



    private KafkaConsumer<String, String> getKafkaConsumer(KafkaContainer kafkaContainer) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        var kafkaConsumer = new KafkaConsumer<String, String>(props);
        kafkaConsumer.subscribe(List.of(EventType.EQUIPMENT_CREATED.getTopicName(), EventType.FILE_MARKED_FOR_DELETION.getTopicName(),
                EventType.EQUIPMENT_DELETED.getTopicName()));
        kafkaConsumer.poll(Duration.ofMillis(100));

        return kafkaConsumer;
    }
}