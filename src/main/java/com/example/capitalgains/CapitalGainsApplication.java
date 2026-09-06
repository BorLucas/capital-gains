package com.example.capitalgains;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CapitalGainsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapitalGainsApplication.class, args);
    }
}
