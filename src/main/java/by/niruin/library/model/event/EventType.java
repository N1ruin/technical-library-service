package by.niruin.library.model.event;

public enum EventType {
    EQUIPMENT_SAVED("equipment-topic"),
    EQUIPMENT_UPDATED(EQUIPMENT_SAVED.getTopicName()),
    EQUIPMENT_DELETED(EQUIPMENT_SAVED.getTopicName()),
    SAFETY_INSTRUCTION_SAVED("instruction-topic"),
    SAFETY_INSTRUCTION_UPDATED(SAFETY_INSTRUCTION_SAVED.getTopicName()),
    SAFETY_INSTRUCTION_DELETED(SAFETY_INSTRUCTION_SAVED.getTopicName()),
    MATERIAL_SAVED("material-topic"),
    MATERIAL_UPDATED(MATERIAL_SAVED.getTopicName()),
    MATERIAL_DELETED(MATERIAL_SAVED.getTopicName()),
    FILE_MOVE_TO_PERMANENT_STORAGE("file-topic"),
    FILE_DELETED_EVENT(FILE_MOVE_TO_PERMANENT_STORAGE.getTopicName());

    private final String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}
