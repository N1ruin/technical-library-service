package by.niruin.library.model.event.material;

import by.niruin.library.model.event.MessageBrokerEvent;

public record MaterialDeletedEvent(String name, String standard) implements MessageBrokerEvent {
}
