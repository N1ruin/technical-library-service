package by.niruin.library.config;

import by.niruin.library.domain.EventType;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration
@TestPropertySource(properties = {"spring.task.scheduling.enabled=false"})
public class TestConfig {
    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    public MinIOContainer minIOContainer() {
        return new MinIOContainer("minio/minio");
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer("apache/kafka:latest");
    }

    @Bean
    public DynamicPropertyRegistrar minioPropertiesRegistrar(MinIOContainer minIOContainer) {
        return (registry) -> {
            registry.add("minio.endpoint", minIOContainer::getS3URL);
            registry.add("minio.user", minIOContainer::getUserName);
            registry.add("minio.password", minIOContainer::getPassword);
            registry.add("minio.bucketName", () -> "equipments");
            registry.add("minio.maxFileSize", () -> 2097152);
        };
    }

    @Bean
    public NewTopic materialCreatedTopic() {
        return TopicBuilder.name(EventType.MATERIAL_CREATED.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
