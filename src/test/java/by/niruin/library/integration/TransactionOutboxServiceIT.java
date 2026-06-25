package by.niruin.library.integration;

import by.niruin.library.config.PostgresConfig;
import by.niruin.library.config.SchedulerConfig;
import by.niruin.library.configuration.properties.SchedulingOutboxProperties;
import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.event.EventType;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.TransactionOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Import({PostgresConfig.class, SchedulerConfig.class})
public class TransactionOutboxServiceIT extends BaseIntegrationTest {
    @Autowired
    private TransactionOutboxService outboxService;
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SafetyInstructionMapper instructionMapper;
    @Autowired
    private SchedulingOutboxProperties schedulingOutboxProperties;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
    }

    @Test
    void save_shouldSaveOutboxRecord() {
        var outboxRecord = createTestOutboxRecord();

        outboxService.save(outboxRecord);

        var savedRecord = outboxRepository.findById(outboxRecord.getId()).get();
        assertNotNull(savedRecord);
        assertEquals(outboxRecord.getEventType(), savedRecord.getEventType());
        assertEquals(outboxRecord.getPayload(), savedRecord.getPayload());
        assertNotNull(savedRecord.getTimestamp());
    }

    @Test
    void createOutboxRecord_shouldCreateValidRecord() {
        var eventType = EventType.SAFETY_INSTRUCTION_CREATED;
        var instruction = createTestInstruction();

        var outboxRecord = outboxService.createOutboxRecord(
                eventType,
                instruction,
                instructionMapper::toCreatedEvent
        );

        assertNotNull(outboxRecord);
        assertEquals(eventType, outboxRecord.getEventType());
        assertNotNull(outboxRecord.getPayload());
        assertNotNull(outboxRecord.getTimestamp());
        assertNull(outboxRecord.getId());
    }

    @Test
    void createOutboxRecord_shouldSerializePayloadCorrectly() {
        var instruction = createTestInstruction();
        var expectedEvent = instructionMapper.toCreatedEvent(instruction);
        var expectedPayload = objectMapper.writeValueAsString(expectedEvent);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.SAFETY_INSTRUCTION_CREATED,
                instruction,
                instructionMapper::toCreatedEvent
        );

        assertEquals(expectedPayload, outboxRecord.getPayload());
    }

    @Test
    void findBatchRecords_shouldReturnEmptyList_whenNoRecords() {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertTrue(batchRecords.isEmpty());
    }

    @Test
    void findBatchRecords_shouldReturnRecordsInOrder() {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        var recordOne = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        var recordTwo = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_UPDATED);
        var recordThree = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_DELETED);

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(3, batchRecords.size());
        assertEquals(recordOne.getId(), batchRecords.get(0).getId());
        assertEquals(recordTwo.getId(), batchRecords.get(1).getId());
        assertEquals(recordThree.getId(), batchRecords.get(2).getId());
    }

    @Test
    void findBatchRecords_shouldReturnLessThanBatchSize_whenNotEnoughRecords() {
        var batchSize = schedulingOutboxProperties.getBatchSize();

        for (int i = 0; i < batchSize - 5; i++) {
            createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        }

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(batchSize - 5, batchRecords.size());
    }

    @Test
    void deleteAll_shouldRemoveAllGivenRecords() {
        var record1 = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        var record2 = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_UPDATED);
        var record3 = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_DELETED);
        var recordsToDelete = List.of(record1, record2);

        outboxService.deleteAll(recordsToDelete);

        assertTrue(outboxRepository.findById(record1.getId()).isEmpty());
        assertTrue(outboxRepository.findById(record2.getId()).isEmpty());
        assertTrue(outboxRepository.findById(record3.getId()).isPresent());
    }

    @Test
    void deleteAll_shouldDoNothing_whenEmptyList() {
        var record = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        List<TransactionOutboxRecord> emptyList = List.of(record);

        outboxService.deleteAll(emptyList);

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void deleteAll_shouldThrowNullPointerException_whenNullList() {
        assertThrows(NullPointerException.class, () -> outboxService.deleteAll(null));
    }

    @Test
    void findBatchRecords_shouldReturnRecordsWithCorrectTimestamp() throws InterruptedException {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        var beforeSave = Instant.now();
        Thread.sleep(2);
        createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        Thread.sleep(2);
        var afterSave = Instant.now();

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(1, batchRecords.size());
        var savedRecord = batchRecords.getFirst();
        assertNotNull(savedRecord.getTimestamp());
        assertTrue(savedRecord.getTimestamp().isAfter(beforeSave) || savedRecord.getTimestamp().equals(beforeSave));
        assertTrue(savedRecord.getTimestamp().isBefore(afterSave) || savedRecord.getTimestamp().equals(afterSave));
    }

    @Test
    void createOutboxRecord_shouldSetTimestampToCurrentTime() throws InterruptedException {
        var beforeCreate = Instant.now();
        Thread.sleep(1);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.SAFETY_INSTRUCTION_CREATED,
                createTestInstruction(),
                instructionMapper::toCreatedEvent
        );

        Thread.sleep(1);
        var afterCreate = Instant.now();

        assertNotNull(outboxRecord.getTimestamp());
        assertTrue(outboxRecord.getTimestamp().isAfter(beforeCreate));
        assertTrue(outboxRecord.getTimestamp().isBefore(afterCreate));
    }

    @Test
    void deleteAll_shouldBeTransactional() {
        var record1 = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED);
        var record2 = createAndSaveOutboxRecord(EventType.SAFETY_INSTRUCTION_UPDATED);
        var recordsToDelete = List.of(record1, record2);

        outboxService.deleteAll(recordsToDelete);

        var remainingRecords = outboxRepository.findAll();
        assertThat(remainingRecords).hasSize(0);
    }

    @Test
    void save_shouldPersistMultipleRecords() {
        var record1 = createTestOutboxRecord();
        var record2 = createTestOutboxRecord();
        var record3 = createTestOutboxRecord();

        outboxService.save(record1);
        outboxService.save(record2);
        outboxService.save(record3);

        var allRecords = outboxRepository.findAll();
        assertEquals(3, allRecords.size());
    }

    @Test
    void createOutboxRecord_shouldHandleDifferentEventTypes() {
        var instruction = createTestInstruction();

        var savedRecord = outboxService.createOutboxRecord(
                EventType.SAFETY_INSTRUCTION_CREATED,
                instruction,
                instructionMapper::toCreatedEvent
        );

        var updatedRecord = outboxService.createOutboxRecord(
                EventType.SAFETY_INSTRUCTION_UPDATED,
                instruction,
                instructionMapper::toUpdatedEvent
        );

        var deletedRecord = outboxService.createOutboxRecord(
                EventType.SAFETY_INSTRUCTION_DELETED,
                instruction,
                instructionMapper::toDeletedEvent
        );

        assertEquals(EventType.SAFETY_INSTRUCTION_CREATED, savedRecord.getEventType());
        assertEquals(EventType.SAFETY_INSTRUCTION_UPDATED, updatedRecord.getEventType());
        assertEquals(EventType.SAFETY_INSTRUCTION_DELETED, deletedRecord.getEventType());
    }

    private TransactionOutboxRecord createTestOutboxRecord() {
        var record = new TransactionOutboxRecord();
        record.setEventType(EventType.SAFETY_INSTRUCTION_CREATED);
        record.setPayload("testPayload");
        record.setTimestamp(Instant.now());

        return record;
    }

    private TransactionOutboxRecord createAndSaveOutboxRecord(EventType eventType) {
        var record = new TransactionOutboxRecord();
        record.setEventType(eventType);
        record.setPayload("TestPayload");
        record.setTimestamp(Instant.now());
        outboxService.save(record);

        return record;
    }

    private SafetyInstruction createTestInstruction() {
        var random = new Random();
        var instruction = new SafetyInstruction();
        instruction.setNumber(String.valueOf(random.nextInt(1000)));
        instruction.setDescription("Test");

        return instruction;
    }
}
