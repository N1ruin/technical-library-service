package by.niruin.library.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration
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
    public DynamicPropertyRegistrar minioPropertiesRegistrar(MinIOContainer minIOContainer) {
        return (registry) -> {
            registry.add("minio.endpoint", minIOContainer::getS3URL);
            registry.add("minio.user", minIOContainer::getUserName);
            registry.add("minio.password", minIOContainer::getPassword);
            registry.add("minio.bucketName", () -> "equipments");
            registry.add("minio.maxFileSize", () -> 2097152);
        };
    }
}
