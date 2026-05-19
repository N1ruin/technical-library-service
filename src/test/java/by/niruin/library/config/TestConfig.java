package by.niruin.library.config;

import by.niruin.library.model.event.EventType;
import com.redis.testcontainers.RedisContainer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.streams.processor.To;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class TestConfig {
    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));
    }

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:8.6-alpine"));
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
        return TopicBuilder.name(EventType.IMAGE_DELETED.getTopicName())
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
