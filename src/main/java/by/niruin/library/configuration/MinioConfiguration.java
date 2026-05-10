package by.niruin.library.configuration;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {
    private static final Logger log = LogManager.getLogger(MinioConfiguration.class);

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        var minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getUser(), properties.getPassword())
                .build();

        createBucket(minioClient, properties);

        return minioClient;
    }

    private void createBucket(MinioClient client, MinioProperties properties) {
        try {
            if (!isBucketExist(client, properties.getBucketName())) {
                client.makeBucket(
                        MakeBucketArgs
                                .builder()
                                .bucket(properties.getBucketName())
                                .build());
            }
        } catch (Exception e) {
            log.fatal("MinIO initialization error: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO initialization error");
        }
    }

    private boolean isBucketExist(MinioClient client, String bucketName) throws MinioException {
        return client.bucketExists(
                io.minio.BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
    }
}
