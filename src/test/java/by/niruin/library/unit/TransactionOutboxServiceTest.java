package by.niruin.library.unit;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.event.EventType;
import by.niruin.library.model.event.instruction.SafetyInstructionCreatedEvent;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.TransactionOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionOutboxServiceTest {
    @Mock
    private TransactionOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private TransactionOutboxService transactionOutboxService;

    @Test
    void save_shouldRepositorySaveInvocation() {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setPayload("Test");
        outboxRecord.setEventType(EventType.EQUIPMENT_CREATED);
        when(outboxRepository.save(outboxRecord)).thenReturn(outboxRecord);

        transactionOutboxService.save(outboxRecord);

        verify(outboxRepository).save(outboxRecord);
    }

    @Test
    void createOutboxRecord_shouldReturnOutboxRecord() {
        var instruction = new SafetyInstruction();
        instruction.setNumber("102");
        instruction.setDescription("test");
        var event = new SafetyInstructionCreatedEvent(instruction.getNumber(), instruction.getDescription(), Instant.now());
        var mapperMock = mock(SafetyInstructionMapper.class);
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        when(mapperMock.toCreatedEvent(instruction)).thenReturn(event);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED,
                instruction, mapperMock::toCreatedEvent);

        assertThat(outboxRecord.getEventType()).isEqualTo(EventType.SAFETY_INSTRUCTION_CREATED);
        assertThat(outboxRecord.getPayload()).isEqualTo("payload");
        assertThat(outboxRecord.getTimestamp().isBefore(Instant.now()));
        verify(objectMapper).writeValueAsString(any());
    }
}
