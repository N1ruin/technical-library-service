package by.niruin.library.model.equipment;

import java.time.LocalDateTime;

public record EquipmentDto(Long id,
                           String name,
                           String index,
                           String description,
                           String imageName,
                           String type,
                           LocalDateTime createdDate,
                           LocalDateTime updatedDate) {
}
