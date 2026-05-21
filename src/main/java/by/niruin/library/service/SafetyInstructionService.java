package by.niruin.library.service;

import by.niruin.library.model.event.EventType;
import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.instruction.UpdateSafetyInstructionRequest;
import by.niruin.library.repository.SafetyInstructionRepository;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyInstructionService {
    private final SafetyInstructionRepository instructionRepository;
    private final SafetyInstructionMapper instructionMapper;
    private final TransactionOutboxService transactionOutboxService;

    public SafetyInstructionService(SafetyInstructionRepository instructionRepository,
                                    SafetyInstructionMapper instructionMapper,
                                    TransactionOutboxService transactionOutboxService) {
        this.instructionRepository = instructionRepository;
        this.instructionMapper = instructionMapper;
        this.transactionOutboxService = transactionOutboxService;
    }

    @Transactional
    @CachePut(value = "instruction", key = "#result.id", unless = "#result == null")
    public SafetyInstruction save(SafetyInstruction instruction) {
        if (instructionRepository.existsByNumber(instruction.getNumber())) {
            throw new EntityAlreadyExistException();
        }

        var savedInstruction = instructionRepository.save(instruction);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_CREATED,
                savedInstruction,
                instructionMapper::toCreatedEvent);

        transactionOutboxService.save(outboxRecord);

        return savedInstruction;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "instruction", key = "#id")
    public SafetyInstruction findById(Long id) {
        return instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<SafetyInstruction> findAll(Pageable pageable) {
        return instructionRepository.findAll(pageable);
    }

    @Transactional
    @CachePut(value = "instruction", key = "#id")
    public SafetyInstruction update(Long id, @Valid UpdateSafetyInstructionRequest request) {
        var instruction = instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (!instruction.getNumber().equals(request.number()) &&
                instructionRepository.existsByNumber(request.number())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(instruction, request);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_UPDATED,
                instruction,
                instructionMapper::toUpdatedEvent);
        transactionOutboxService.save(outboxRecord);

        return instruction;
    }

    @Transactional
    @CacheEvict(value = "instruction", key = "#id")
    public void deleteById(Long id) {
        var instruction = instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        instructionRepository.delete(instruction);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_DELETED,
                instruction,
                instructionMapper::toDeletedEvent);
        transactionOutboxService.save(outboxRecord);
    }

    private void updateFields(SafetyInstruction oldInstruction, UpdateSafetyInstructionRequest request) {
        oldInstruction.setNumber(request.number());
        oldInstruction.setDescription(request.description());
    }
}
