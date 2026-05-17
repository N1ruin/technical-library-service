package by.niruin.library.model.event.instruction;

import by.niruin.library.model.event.MessageBrokerEvent;

public record SafetyInstructionDeletedEvent(String number) implements MessageBrokerEvent {
}
