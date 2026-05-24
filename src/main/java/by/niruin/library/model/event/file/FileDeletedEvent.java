package by.niruin.library.model.event.file;

import by.niruin.library.model.event.MessageBrokerEvent;

public record FileDeletedEvent(String fileName, String eventType) implements MessageBrokerEvent {
}
