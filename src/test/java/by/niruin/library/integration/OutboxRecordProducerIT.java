package by.niruin.library.integration;

import by.niruin.library.config.KafkaConfig;
import by.niruin.library.config.PostgresConfig;
import by.niruin.library.config.SchedulerConfig;
import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.event.EventType;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.scheduler.OutboxRecordScheduler;
import by.niruin.library.service.TransactionOutboxService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import({PostgresConfig.class, KafkaConfig.class, SchedulerConfig.class})
public class OutboxRecordProducerIT extends BaseIntegrationTest {
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private TransactionOutboxService outboxService;
    @Autowired
    private SafetyInstructionMapper instructionMapper;
    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private OutboxRecordScheduler scheduler;

    @Test
    void produce_shouldAllMessagesSendSuccessful() {
        var records = getTestInstructions();
        outboxRepository.saveAll(records);

        var consumerRecords = new ArrayList<ConsumerRecord<String, String>>();

        try (var kafkaConsumer = getKafkaConsumer(kafkaContainer)) {
            await().atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofSeconds(1))
                    .until(() -> {
                        scheduler.sendOutboxEvents();
                        var polled = kafkaConsumer.poll(Duration.ofMillis(1));
                        polled.forEach(consumerRecords::add);

                        return consumerRecords.size() == 3;
                    });
        }

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    private List<TransactionOutboxRecord> getTestInstructions() {
        var instructionOne = new SafetyInstruction();
        instructionOne.setNumber("1");
        instructionOne.setDescription("test1");

        var instructionTwo = new SafetyInstruction();
        instructionTwo.setNumber("2");
        instructionTwo.setDescription("test2");

        var instructionThree = new SafetyInstruction();
        instructionThree.setNumber("3");
        instructionThree.setDescription("test3");

        return Stream.of(instructionOne, instructionTwo, instructionThree)
                .map(instruction -> outboxService.createOutboxRecord(
                        EventType.SAFETY_INSTRUCTION_SAVED,
                        instruction,
                        instructionMapper::toCreatedEvent))
                .toList();
    }

    private KafkaConsumer<String, String> getKafkaConsumer(KafkaContainer kafkaContainer) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var kafkaConsumer = new KafkaConsumer<String, String>(props);
        kafkaConsumer.subscribe(List.of(EventType.SAFETY_INSTRUCTION_SAVED.getTopicName()));
        kafkaConsumer.poll(Duration.ofMillis(1));
        return kafkaConsumer;
    }
}
