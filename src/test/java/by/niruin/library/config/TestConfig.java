package by.niruin.library.config;

import by.niruin.library.domain.EventType;
import com.redis.testcontainers.RedisContainer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
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
    public MinIOContainer minIOContainer() {
        return new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1"));
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));
    }

    @Bean
    public DynamicPropertyRegistrar minioPropertiesRegistrar(MinIOContainer minIOContainer) {
        return registry -> {
            registry.add("minio.endpoint", minIOContainer::getS3URL);
            registry.add("minio.user", minIOContainer::getUserName);
            registry.add("minio.password", minIOContainer::getPassword);
            registry.add("minio.bucketName", () -> "equipments");
            registry.add("minio.maxFileSize", () -> 2097152);
        };
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
}
