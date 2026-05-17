package by.niruin.library.model.event.material;

import by.niruin.library.model.event.MessageBrokerEvent;

import java.time.Instant;
import java.time.LocalDateTime;

public record MaterialUpdatedEvent(String name,
                                   String description,
                                   String standard,
                                   String supplierCode,
                                   Instant updatedDate) implements MessageBrokerEvent {
}
