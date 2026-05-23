package by.niruin.library.configuration;

import by.niruin.library.configuration.properties.SchedulingOutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SchedulingOutboxProperties.class)
@Profile("!test")
public class SchedulingConfiguration {
}
