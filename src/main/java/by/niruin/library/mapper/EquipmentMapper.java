package by.niruin.library.mapper;

import by.niruin.library.domain.Equipment;
import by.niruin.library.model.equipment.CreateEquipmentRequest;
import by.niruin.library.model.equipment.CreateEquipmentResponse;
import by.niruin.library.model.equipment.EquipmentDto;
import by.niruin.library.model.event.equipment.EquipmentCreatedEvent;
import by.niruin.library.model.event.equipment.EquipmentUpdatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {
    @Mapping(target = "imageName", ignore = true)
    Equipment toEquipment(CreateEquipmentRequest request);

    CreateEquipmentResponse toCreateResponse(Equipment equipment);

    EquipmentDto toDto(Equipment equipment);

    List<EquipmentDto> toDtoList(List<Equipment> equipments);

    EquipmentCreatedEvent toCreatedEvent(Equipment equipment);

    EquipmentUpdatedEvent toUpdatedEvent(Equipment equipment);
}
