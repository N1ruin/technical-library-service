package by.niruin.library.model.event.material;

import by.niruin.library.model.event.KafkaEvent;

import java.time.LocalDateTime;

public record MaterialCreatedEvent(Long id,
                                   String name,
                                   String description,
                                   String standard,
                                   String supplierCode,
                                   LocalDateTime createdDate) implements KafkaEvent {
}
