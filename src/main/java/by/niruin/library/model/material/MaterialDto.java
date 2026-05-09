package by.niruin.library.model.material;

import java.time.LocalDateTime;

public record MaterialDto(Long id,
                          String name,
                          String description,
                          String standard,
                          String supplierCode,
                          LocalDateTime createdDate,
                          LocalDateTime updatedDate) {
}
