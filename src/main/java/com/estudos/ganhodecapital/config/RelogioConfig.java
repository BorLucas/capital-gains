package com.estudos.ganhodecapital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelogioConfig {

    /** Injetavel onde precisar de "agora", para os testes poderem fixar o tempo. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
