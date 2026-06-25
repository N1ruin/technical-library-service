package by.niruin.library.config;

import by.niruin.library.model.event.EventType;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class KafkaConfig {
    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));
    }

    @Bean
    public NewTopic materialTopic() {
        return TopicBuilder.name(EventType.MATERIAL_CREATED.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fileDeletionTopic() {
        return TopicBuilder.name(EventType.FILE_DELETED_EVENT.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic instructionTopic() {
        return TopicBuilder.name(EventType.SAFETY_INSTRUCTION_DELETED.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic equipmentTopic() {
        return TopicBuilder.name(EventType.EQUIPMENT_DELETED.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
