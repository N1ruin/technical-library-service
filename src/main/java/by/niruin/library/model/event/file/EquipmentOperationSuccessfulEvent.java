package by.niruin.library.model.event.file;

import by.niruin.library.model.event.MessageBrokerEvent;

public record EquipmentOperationSuccessfulEvent(String fileName) implements MessageBrokerEvent {
}
