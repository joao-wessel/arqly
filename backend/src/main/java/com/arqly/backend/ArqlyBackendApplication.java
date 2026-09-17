package com.arqly.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArqlyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArqlyBackendApplication.class, args);
    }
}
