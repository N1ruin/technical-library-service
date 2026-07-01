package by.niruin.library.mapper;

import by.niruin.library.domain.Material;
import by.niruin.library.model.event.material.MaterialCreatedEvent;
import by.niruin.library.model.event.material.MaterialDeletedEvent;
import by.niruin.library.model.event.material.MaterialUpdatedEvent;
import by.niruin.library.model.material.CreateMaterialRequest;
import by.niruin.library.model.material.CreateMaterialResponse;
import by.niruin.library.model.material.MaterialDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = InstantMapper.class)
public interface MaterialMapper {
    Material toMaterial(CreateMaterialRequest request);

    CreateMaterialResponse toCreateResponse(Material material);

    MaterialDto toDto(Material material);

    MaterialCreatedEvent toSavedEvent(Material material);

    MaterialUpdatedEvent toUpdatedEvent(Material material);

    MaterialDeletedEvent toDeleteEvent(Material material);
}
