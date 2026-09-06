package com.estudos.ganhodecapital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ganhoDeCapitalOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Ganho de Capital")
                .version("0.0.1")
                .description("""
                        Calculo de imposto sobre lucros/prejuizos em operacoes de acoes
                        (desafio "Ganho de Capital"). Projeto de estudo de Spring Boot.

                        Swagger UI: /swagger-ui.html - Console do H2: /h2-console
                        """));
    }
}
