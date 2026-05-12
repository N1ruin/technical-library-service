package by.niruin.library.model.event.material;

import by.niruin.library.model.event.KafkaEvent;

public record MaterialDeletedEvent(String name, String standard) implements KafkaEvent {
}
