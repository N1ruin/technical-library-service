package by.niruin.library.unit;

import by.niruin.library.model.event.EventType;
import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.instruction.UpdateSafetyInstructionRequest;
import by.niruin.library.repository.SafetyInstructionRepository;
import by.niruin.library.service.SafetyInstructionService;
import by.niruin.library.service.TransactionOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class SafetyInstructionServiceTest {
    @Mock
    private SafetyInstructionRepository instructionRepository;
    @Mock
    private SafetyInstructionMapper safetyInstructionMapper;
    @Mock
    private TransactionOutboxService outboxService;
    @InjectMocks
    private SafetyInstructionService instructionService;

    @Test
    void saveSuccess_shouldReturnSavedInstruction() {
        var instruction = createTestInstruction();
        when(instructionRepository.existsByNumber(instruction.getNumber()))
                .thenReturn(false);
        when(instructionRepository.save(instruction))
                .thenReturn(instruction);
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_CREATED), eq(instruction), any()))
                .thenReturn(outboxRecord);

        var saved = instructionService.save(instruction);

        assertThat(instruction).usingRecursiveComparison()
                .isEqualTo(saved);
        verify(outboxService).createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_CREATED), eq(instruction), any());
        verify(instructionRepository).existsByNumber(instruction.getNumber());
        verify(instructionRepository).save(instruction);
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void save_shouldThrowsEntityAlreadyExist() {
        var instruction = createTestInstruction();
        when(instructionRepository.existsByNumber(instruction.getNumber()))
                .thenReturn(true);

        assertThatThrownBy(() -> instructionService.save(instruction))
                .isInstanceOf(EntityAlreadyExistException.class);

        verify(instructionRepository).existsByNumber(instruction.getNumber());
        verify(instructionRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void findById_shouldReturnInstruction() {
        var instruction = createTestInstruction();
        var id = 1L;
        when(instructionRepository.findById(id))
                .thenReturn(Optional.of(instruction));

        var found = instructionService.findById(id);

        assertThat(instruction).usingRecursiveComparison()
                .isEqualTo(found);
        verify(instructionRepository).findById(id);
    }

    @Test
    void findById_shouldThrowsEntityNotFound() {
        var id = 10L;
        when(instructionRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructionService.findById(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(instructionRepository).findById(id);
        verifyNoInteractions(outboxService);
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(instructionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());
        var pageRequest = PageRequest.of(0, 10);

        var materialPage = instructionService.findAll(pageRequest);

        assertThat(materialPage).hasSize(0);
        verify(instructionRepository).findAll(any(PageRequest.class));
    }

    @Test
    void findAll_shouldReturnListWithSizeOne() {
        var instruction = createTestInstruction();
        var expectedPage = new PageImpl<>(List.of(instruction));
        when(instructionRepository.findAll(any(Pageable.class)))
                .thenReturn(expectedPage);
        var pageRequest = PageRequest.of(0, 10);

        var materialPage = instructionService.findAll(pageRequest);

        assertThat(materialPage).hasSize(1);
        assertThat(materialPage.getContent().getFirst()).usingRecursiveComparison()
                .isEqualTo(instruction);
        verify(instructionRepository).findAll(any(PageRequest.class));
    }

    @Test
    void update_shouldReturnUpdatedInstruction() {
        var id = 1L;
        var instruction = createTestInstruction();
        var updateRequest = createUpdateInstructionRequest();
        when(instructionRepository.findById(id))
                .thenReturn(Optional.of(instruction));
        when(instructionRepository.existsByNumber(updateRequest.number()))
                .thenReturn(false);
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_UPDATED), eq(instruction), any()))
                .thenReturn(outboxRecord);

        var updated = instructionService.update(id, updateRequest);

        assertThat(updated.getNumber()).isEqualTo(updateRequest.number());
        assertThat(updated.getDescription()).isEqualTo(updateRequest.description());
        verify(instructionRepository).findById(id);
        verify(instructionRepository).existsByNumber(updateRequest.number());
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void update_shouldThrowEntityNotFound() {
        var id = 1L;
        var updateRequest = createUpdateInstructionRequest();
        when(instructionRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructionService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);

        verify(instructionRepository).findById(id);
        verify(instructionRepository, never()).existsByNumber(updateRequest.number());
        verifyNoInteractions(outboxService);
    }

    @Test
    void update_shouldThrowEntityAlreadyExist_whenNewNumberExist() {
        var id = 1L;
        var updateRequest = createUpdateInstructionRequest();
        when(instructionRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructionService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);
        verify(instructionRepository).findById(id);
        verify(instructionRepository, never()).existsByNumber(updateRequest.number());
        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteById_shouldDeleteSuccess() {
        var id = 1L;
        var instruction = createTestInstruction();
        when(instructionRepository.findById(id))
                .thenReturn(Optional.of(instruction));
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_DELETED), eq(instruction), any()))
                .thenReturn(outboxRecord);

        instructionService.deleteById(id);

        verify(outboxService).createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_DELETED), eq(instruction), any());
        verify(instructionRepository).findById(id);
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void deleteById_shouldThrowEntityNotFound() {
        var id = 1L;
        when(instructionRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructionService.deleteById(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(instructionRepository).findById(id);
        verify(instructionRepository, never()).deleteById(id);
        verifyNoInteractions(outboxService);
    }

    private SafetyInstruction createTestInstruction() {
        var instruction = new SafetyInstruction();
        instruction.setNumber("123п");
        instruction.setDescription("Для слесарей механосборочных работ");

        return instruction;
    }

    private UpdateSafetyInstructionRequest createUpdateInstructionRequest() {
        var number = "322";
        var description = "Для слесарей МСР";

        return new UpdateSafetyInstructionRequest(number, description);
    }
}
