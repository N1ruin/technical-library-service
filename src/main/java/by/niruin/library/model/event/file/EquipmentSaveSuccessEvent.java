package by.niruin.library.model.event.file;

import by.niruin.library.model.event.MessageBrokerEvent;

public record EquipmentSaveSuccessEvent(String fileName) implements MessageBrokerEvent {
}
