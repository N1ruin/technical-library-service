package by.niruin.library.model.event.instruction;

import java.time.LocalDateTime;

public record SafetyInstructionUpdatedEvent(Long id,
                                            String number,
                                            String description,
                                            LocalDateTime updatedDate) {
}
