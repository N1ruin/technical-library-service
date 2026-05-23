package by.niruin.library.model.event.material;

import by.niruin.library.model.event.MessageBrokerEvent;

import java.time.Instant;

public record MaterialCreatedEvent(String name,
                                   String description,
                                   String standard,
                                   String supplierCode,
                                   Instant createdDate) implements MessageBrokerEvent {
}
