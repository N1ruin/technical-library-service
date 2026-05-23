package by.niruin.library.unit;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.kafka.EventPublisher;
import by.niruin.library.model.instruction.UpdateSafetyInstructionRequest;
import by.niruin.library.repository.SafetyInstructionRepository;
import by.niruin.library.service.SafetyInstructionService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SafetyInstructionServiceTest {
    @Mock
    private SafetyInstructionRepository instructionRepository;
    @Mock
    private EventPublisher eventPublisher;
    @InjectMocks
    private SafetyInstructionService instructionService;

    @Test
    void saveSuccess_shouldReturnSavedInstruction() {
        var instruction = createTestInstruction();
        when(instructionRepository.existsByNumber(instruction.getNumber()))
                .thenReturn(false);
        when(instructionRepository.save(instruction))
                .thenReturn(instruction);

        var saved = instructionService.save(instruction);

        assertThat(instruction).usingRecursiveComparison()
                .isEqualTo(saved);
        verify(eventPublisher).publishInstructionSavedEvent(instruction);
        verify(instructionRepository).existsByNumber(instruction.getNumber());
        verify(instructionRepository).save(instruction);
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
        verifyNoInteractions(eventPublisher);
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

        var updated = instructionService.update(id, updateRequest);

        assertThat(updated.getNumber()).isEqualTo(updateRequest.number());
        assertThat(updated.getDescription()).isEqualTo(updateRequest.description());
        verify(instructionRepository).findById(id);
        verify(instructionRepository).existsByNumber(updateRequest.number());
        verify(eventPublisher).publishInstructionUpdatedEvent(updated);
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
        verifyNoInteractions(eventPublisher);
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteById_shouldDeleteSuccess() {
        var id = 1L;
        var instruction = createTestInstruction();
        when(instructionRepository.findById(id))
                .thenReturn(Optional.of(instruction));

        instructionService.deleteById(id);

        verify(eventPublisher).publishInstructionDeletedEvent(instruction);
        verify(instructionRepository).findById(id);
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
        verifyNoInteractions(eventPublisher);
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
