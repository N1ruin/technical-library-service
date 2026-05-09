package by.niruin.library.service;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.instruction.UpdateSafetyInstructionRequest;
import by.niruin.library.repository.SafetyInstructionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SafetyInstructionService {
    private final SafetyInstructionRepository instructionRepository;
    private final SafetyInstructionMapper instructionMapper;

    public SafetyInstructionService(SafetyInstructionRepository instructionRepository,
                                    SafetyInstructionMapper instructionMapper) {
        this.instructionRepository = instructionRepository;
        this.instructionMapper = instructionMapper;
    }

    public SafetyInstruction save(SafetyInstruction instruction) {
        if (instructionRepository.existsByNumber(instruction.getNumber())) {
            throw new EntityAlreadyExistException();
        }
//        var createdEvent = instructionMapper.toCreatedEvent(instruction);//todo transaction outbox
//        kafkaTemplate.send(MATERIAL_TOPIC, createdEvent);
        return instructionRepository.save(instruction);
    }

    @Transactional(readOnly = true)
    public SafetyInstruction findById(Long id) {
        return instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<SafetyInstruction> findAll() {
        return instructionRepository.findAll();
    }

    public SafetyInstruction update(Long id, @Valid UpdateSafetyInstructionRequest request) {
        var instruction = instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (instructionRepository.existsByNumber(request.number())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(instruction, request);
//        var updatedEvent = materialMapper.toUpdatedEvent(material);//todo transaction outbox
//        kafkaTemplate.send(MATERIAL_TOPIC, event);

        return instruction;
    }

    public void deleteById(Long id) {
        var instruction = instructionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

//        var deleteEvent = instructionMapper.toCreatedEvent(instruction);//todo transaction outbox
//        kafkaTemplate.send(MATERIAL_TOPIC, createdEvent);

        instructionRepository.delete(instruction);
    }

    private void updateFields(SafetyInstruction oldInstruction, UpdateSafetyInstructionRequest request) {
        oldInstruction.setNumber(request.number());
        oldInstruction.setDescription(request.description());
    }
}
