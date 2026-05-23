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
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionOutboxServiceTest {
    @Mock
    private TransactionOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private TransactionOutboxService transactionOutboxService;

    @Test
    void save_shouldRepositorySaveInvocation() {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setPayload("Test");
        outboxRecord.setEventType(EventType.EQUIPMENT_SAVED);
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

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_SAVED,
                instruction, mapperMock::toCreatedEvent);

        assertThat(outboxRecord.getEventType()).isEqualTo(EventType.SAFETY_INSTRUCTION_SAVED);
        assertThat(outboxRecord.getPayload()).isEqualTo("payload");
        assertThat(outboxRecord.getTimestamp().isBefore(Instant.now()));
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void deleteAll_shouldThrowNullPointerEx() {
        assertThatThrownBy(() -> transactionOutboxService.deleteAll(null))
                .isInstanceOf(NullPointerException.class);
        verify(outboxRepository, never()).deleteAll(any());
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    void deleteAll_shouldRepositoryDontInvocation() {
        var emptyList = new ArrayList<TransactionOutboxRecord>();

        transactionOutboxService.deleteAll(emptyList);

        verify(outboxRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAll_shouldRepositoryInvocation() {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setPayload("testPayload");
        outboxRecord.setEventType(EventType.SAFETY_INSTRUCTION_SAVED);
        var list = List.of(outboxRecord);
        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        transactionOutboxService.deleteAll(list);

        verify(outboxRepository).deleteAll(list);
    }
}
