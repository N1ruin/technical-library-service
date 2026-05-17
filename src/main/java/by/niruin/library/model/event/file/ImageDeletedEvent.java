package by.niruin.library.model.event.file;

import by.niruin.library.model.event.MessageBrokerEvent;

public record ImageDeletedEvent(String fileName) implements MessageBrokerEvent {
}
