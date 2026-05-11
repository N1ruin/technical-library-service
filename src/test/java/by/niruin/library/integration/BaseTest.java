package by.niruin.library.integration;

import by.niruin.library.config.TestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = TestConfig.class)
@Testcontainers
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Transactional
public class BaseTest {
    @Autowired
    static PostgreSQLContainer postgreSQLContainer;
    @Autowired
    static KafkaContainer kafkaContainer;
}
