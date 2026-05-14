package by.niruin.library.integration;

import by.niruin.library.config.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import(TestConfig.class)
@Testcontainers
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
}
