package by.niruin.library.model.event.material;

import java.time.LocalDateTime;

public record MaterialUpdatedEvent(Long id,
                                   String name,
                                   String description,
                                   String standard,
                                   String supplierCode,
                                   LocalDateTime updatedDate) {
}
