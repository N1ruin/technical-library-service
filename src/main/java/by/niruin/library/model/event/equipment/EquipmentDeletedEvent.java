package by.niruin.library.model.event.equipment;

import by.niruin.library.model.event.MessageBrokerEvent;

public record EquipmentDeletedEvent(String name,
                                    String index) implements MessageBrokerEvent {
}
