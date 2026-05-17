package by.niruin.library.model.event.equipment;

import by.niruin.library.domain.EquipmentType;
import by.niruin.library.model.event.MessageBrokerEvent;

public record EquipmentCreatedEvent(String name,
                                    String index,
                                    String description,
                                    EquipmentType type) implements MessageBrokerEvent {
}
