package by.niruin.library.model.event;

public enum EventType {
    EQUIPMENT_CREATED("equipment-topic"),
    EQUIPMENT_UPDATED(EQUIPMENT_CREATED.getTopicName()),
    EQUIPMENT_DELETED(EQUIPMENT_CREATED.getTopicName()),
    SAFETY_INSTRUCTION_CREATED("instruction-topic"),
    SAFETY_INSTRUCTION_UPDATED(SAFETY_INSTRUCTION_CREATED.getTopicName()),
    SAFETY_INSTRUCTION_DELETED(SAFETY_INSTRUCTION_CREATED.getTopicName()),
    MATERIAL_CREATED("material-topic"),
    MATERIAL_UPDATED(MATERIAL_CREATED.getTopicName()),
    MATERIAL_DELETED(MATERIAL_CREATED.getTopicName()),
    EQUIPMENT_SAVE_SUCCESS_EVENT("file-topic");

    private final String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}
