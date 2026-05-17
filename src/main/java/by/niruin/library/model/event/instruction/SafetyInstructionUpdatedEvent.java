package by.niruin.library.model.event.instruction;

import by.niruin.library.model.event.MessageBrokerEvent;

import java.time.LocalDateTime;

public record SafetyInstructionUpdatedEvent(String number,
                                            String description,
                                            LocalDateTime updatedDate) implements MessageBrokerEvent {
}
