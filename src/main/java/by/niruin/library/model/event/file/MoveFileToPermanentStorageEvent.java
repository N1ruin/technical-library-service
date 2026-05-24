package by.niruin.library.model.event.file;

import by.niruin.library.model.event.MessageBrokerEvent;

public record MoveFileToPermanentStorageEvent(String fileName, String eventType) implements MessageBrokerEvent {
}
