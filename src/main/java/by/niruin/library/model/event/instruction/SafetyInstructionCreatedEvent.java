package by.niruin.library.model.event.instruction;

import by.niruin.library.model.event.MessageBrokerEvent;

import java.time.Instant;

public record SafetyInstructionCreatedEvent(String number,
                                            String description,
                                            Instant createdDate) implements MessageBrokerEvent {
}
