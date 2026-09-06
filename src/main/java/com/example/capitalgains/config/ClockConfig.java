package com.example.capitalgains.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injectable wherever "now" is needed, so tests can freeze time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
