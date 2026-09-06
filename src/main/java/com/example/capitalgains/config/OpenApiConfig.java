package com.example.capitalgains.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI capitalGainsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Capital Gains")
                .version("0.0.1")
                .description("""
                        Tax calculation on profits/losses from stock trades
                        (the "Capital Gains" challenge). Spring Boot study project.

                        Swagger UI: /swagger-ui.html - H2 console: /h2-console
                        """));
    }
}
