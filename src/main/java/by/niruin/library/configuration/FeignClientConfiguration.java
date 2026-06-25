package by.niruin.library.configuration;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@EnableFeignClients(basePackages = "by.niruin.library.client")
@Configuration
public class FeignClientConfiguration {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                requestTemplate.header("Authorization",
                        "Bearer " + jwtAuthenticationToken.getToken().getTokenValue());
            }
        };
    }

    @Bean
    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return ((feignClientName, target, method) -> feignClientName);
    }
}
