package by.niruin.library.mapper;

import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.model.event.equipment.EquipmentCreatedEvent;
import by.niruin.library.model.event.equipment.EquipmentDeletedEvent;
import by.niruin.library.model.event.equipment.EquipmentUpdatedEvent;
import by.niruin.library.model.event.file.FileDeletedEvent;
import by.niruin.library.model.event.file.MoveFileToPermanentStorageEvent;
import by.niruin.library.model.event.instruction.SafetyInstructionCreatedEvent;
import by.niruin.library.model.event.instruction.SafetyInstructionDeletedEvent;
import by.niruin.library.model.event.instruction.SafetyInstructionUpdatedEvent;
import by.niruin.library.model.event.material.MaterialSavedEvent;
import by.niruin.library.model.event.material.MaterialDeletedEvent;
import by.niruin.library.model.event.material.MaterialUpdatedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxRecordMapper {
    private final ObjectMapper objectMapper;

    public OutboxRecordMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object mapRecordToJson(TransactionOutboxRecord record) {
        var eventType = record.getEventType();
        String payload = record.getPayload();

        return switch (eventType) {
            case FILE_MOVE_TO_PERMANENT_STORAGE ->
                    objectMapper.readValue(payload, MoveFileToPermanentStorageEvent.class);
            case FILE_DELETED_EVENT -> objectMapper.readValue(payload, FileDeletedEvent.class);
            case EQUIPMENT_SAVED -> objectMapper.readValue(payload, EquipmentCreatedEvent.class);
            case EQUIPMENT_UPDATED -> objectMapper.readValue(payload, EquipmentUpdatedEvent.class);
            case EQUIPMENT_DELETED -> objectMapper.readValue(payload, EquipmentDeletedEvent.class);
            case SAFETY_INSTRUCTION_SAVED -> objectMapper.readValue(payload, SafetyInstructionCreatedEvent.class);
            case SAFETY_INSTRUCTION_UPDATED -> objectMapper.readValue(payload, SafetyInstructionUpdatedEvent.class);
            case SAFETY_INSTRUCTION_DELETED -> objectMapper.readValue(payload, SafetyInstructionDeletedEvent.class);
            case MATERIAL_SAVED -> objectMapper.readValue(payload, MaterialSavedEvent.class);
            case MATERIAL_UPDATED -> objectMapper.readValue(payload, MaterialUpdatedEvent.class);
            case MATERIAL_DELETED -> objectMapper.readValue(payload, MaterialDeletedEvent.class);
        };
    }
}
