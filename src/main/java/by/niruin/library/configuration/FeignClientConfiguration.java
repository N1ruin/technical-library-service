package by.niruin.library.configuration;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients(basePackages = "by.niruin.library.client")
@Configuration
public class FeignClientConfiguration {
}
