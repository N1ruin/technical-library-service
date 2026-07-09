package by.niruin.library.model.event;

public enum EventType {
    EQUIPMENT_CREATED("technical-library-topic"),
    EQUIPMENT_UPDATED(EQUIPMENT_CREATED.getTopicName()),
    EQUIPMENT_DELETED(EQUIPMENT_CREATED.getTopicName()),
    SAFETY_INSTRUCTION_CREATED("technical-library-topic"),
    SAFETY_INSTRUCTION_UPDATED(SAFETY_INSTRUCTION_CREATED.getTopicName()),
    SAFETY_INSTRUCTION_DELETED(SAFETY_INSTRUCTION_CREATED.getTopicName()),
    MATERIAL_CREATED("technical-library-topic"),
    MATERIAL_UPDATED(MATERIAL_CREATED.getTopicName()),
    MATERIAL_DELETED(MATERIAL_CREATED.getTopicName()),
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
