package by.niruin.library.model.instruction;

import java.time.LocalDateTime;

public record SafetyInstructionDto(Long id,
                                   String number,
                                   String description,
                                   LocalDateTime createdDate,
                                   LocalDateTime updatedDate) {
}
