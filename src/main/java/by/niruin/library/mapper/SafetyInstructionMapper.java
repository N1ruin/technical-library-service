package by.niruin.library.mapper;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.model.event.instruction.SafetyInstructionCreatedEvent;
import by.niruin.library.model.event.instruction.SafetyInstructionDeletedEvent;
import by.niruin.library.model.event.instruction.SafetyInstructionUpdatedEvent;
import by.niruin.library.model.instruction.CreateSafetyInstructionRequest;
import by.niruin.library.model.instruction.CreateSafetyInstructionResponse;
import by.niruin.library.model.instruction.SafetyInstructionDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = InstantMapper.class)
public interface SafetyInstructionMapper {
    SafetyInstruction toInstruction(CreateSafetyInstructionRequest request);

    CreateSafetyInstructionResponse toCreateResponse(SafetyInstruction instruction);

    SafetyInstructionDto toDto(SafetyInstruction instruction);

    List<SafetyInstructionDto> toDtoList(List<SafetyInstruction> instructions);

    SafetyInstructionCreatedEvent toCreatedEvent(SafetyInstruction instruction);

    SafetyInstructionUpdatedEvent toUpdatedEvent(SafetyInstruction instruction);

    SafetyInstructionDeletedEvent toDeletedEvent(SafetyInstruction instruction);
}
