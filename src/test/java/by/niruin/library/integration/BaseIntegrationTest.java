package by.niruin.library.integration;

import by.niruin.library.config.KafkaConfig;
import by.niruin.library.config.PostgresConfig;
import by.niruin.library.config.RedisConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Import({KafkaConfig.class, PostgresConfig.class, RedisConfig.class})
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
}
