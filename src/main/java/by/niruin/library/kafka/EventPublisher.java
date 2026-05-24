package by.niruin.library.kafka;

import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.Material;
import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.event.EventType;
import by.niruin.library.model.event.file.MoveFileToPermanentStorageEvent;
import by.niruin.library.model.event.file.FileDeletedEvent;
import by.niruin.library.service.TransactionOutboxService;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final TransactionOutboxService outboxService;
    private final EquipmentMapper equipmentMapper;
    private final MaterialMapper materialMapper;
    private final SafetyInstructionMapper instructionMapper;

    public EventPublisher(TransactionOutboxService outboxService, EquipmentMapper equipmentMapper,
                          MaterialMapper materialMapper, SafetyInstructionMapper instructionMapper) {
        this.outboxService = outboxService;
        this.equipmentMapper = equipmentMapper;
        this.materialMapper = materialMapper;
        this.instructionMapper = instructionMapper;
    }

    public void publishEquipmentSavedEvent(Equipment equipment) {
        var equipmentCreatedEvent = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_SAVED,
                equipment,
                equipmentMapper::toCreatedEvent);
        outboxService.save(equipmentCreatedEvent);
    }

    public void publishEquipmentDeletedEvent(Equipment equipment) {
        var equipmentDeletionEvent = outboxService.createOutboxRecord(EventType.EQUIPMENT_DELETED,
                equipment,
                equipmentMapper::toDeletedEvent);
        outboxService.save(equipmentDeletionEvent);
    }

    public void publishEquipmentUpdatedEvent(Equipment equipment) {
        var notificationOutboxRecord = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_UPDATED,
                equipment,
                equipmentMapper::toUpdatedEvent);
        outboxService.save(notificationOutboxRecord);
    }

    public void publishFileMovedToPermanentStorageEvent(String fileName) {
        var moveFileOutboxRecord = outboxService.createOutboxRecord(
                EventType.FILE_MOVE_TO_PERMANENT_STORAGE,
                fileName,
                (name) ->
                        new MoveFileToPermanentStorageEvent(name, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name()));
        outboxService.save(moveFileOutboxRecord);
    }

    public void publishFileDeletedEvent(String oldFileName) {
        if (oldFileName != null) {
            var deleteFileOutboxRecord = outboxService.createOutboxRecord(
                    EventType.FILE_DELETED_EVENT,
                    oldFileName,
                    (name) ->
                            new FileDeletedEvent(name, EventType.FILE_DELETED_EVENT.name()));
            outboxService.save(deleteFileOutboxRecord);
        }
    }

    public void publishMaterialSavedEvent(Material material) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.MATERIAL_SAVED,
                material,
                materialMapper::toSavedEvent);
        outboxService.save(outboxRecord);
    }

    public void publishMaterialUpdatedEvent(Material material) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.MATERIAL_UPDATED,
                material,
                materialMapper::toUpdatedEvent);
        outboxService.save(outboxRecord);
    }

    public void publishMaterialDeletedEvent(Material material) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.MATERIAL_DELETED,
                material,
                materialMapper::toDeleteEvent);
        outboxService.save(outboxRecord);
    }

    public void publishInstructionSavedEvent(SafetyInstruction instruction) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_SAVED,
                instruction,
                instructionMapper::toCreatedEvent);
        outboxService.save(outboxRecord);
    }

    public void publishInstructionUpdatedEvent(SafetyInstruction instruction) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_UPDATED,
                instruction,
                instructionMapper::toUpdatedEvent);
        outboxService.save(outboxRecord);
    }

    public void publishInstructionDeletedEvent(SafetyInstruction instruction) {
        var outboxRecord = outboxService.createOutboxRecord(EventType.SAFETY_INSTRUCTION_DELETED,
                instruction,
                instructionMapper::toDeletedEvent);
        outboxService.save(outboxRecord);
    }
}
