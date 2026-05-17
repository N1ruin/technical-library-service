package by.niruin.library.model.event.equipment;

import by.niruin.library.domain.EquipmentType;
import by.niruin.library.model.event.MessageBrokerEvent;

import java.time.Instant;

public record EquipmentUpdatedEvent(String name,
                                    String index,
                                    String description,
                                    EquipmentType type,
                                    Instant updatedDate) implements MessageBrokerEvent {
}
