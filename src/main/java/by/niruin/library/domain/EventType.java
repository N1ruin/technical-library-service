package by.niruin.library.domain;

public enum EventType {
    EQUIPMENT_CREATED("equipment-topic"),
    EQUIPMENT_UPDATED("equipment-topic"),
    SAFETY_INSTRUCTION_CREATED("instruction-topic"),
    SAFETY_INSTRUCTION_UPDATED("instruction-topic"),
    MATERIAL_CREATED("material-topic"),
    MATERIAL_UPDATED("material-topic");

    private String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}
