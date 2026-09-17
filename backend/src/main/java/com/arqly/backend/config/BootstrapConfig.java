package com.arqly.backend.config;

import com.arqly.backend.service.InitialPlatformBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapConfig {
    @Bean
    CommandLineRunner bootstrapInitialPlatformAdmin(InitialPlatformBootstrapService service,
                                             @Value("${arqly.initial-bootstrap.enabled:false}") boolean enabled,
                                             @Value("${arqly.initial-bootstrap.admin-name:}") String adminName,
                                             @Value("${arqly.initial-bootstrap.admin-email:}") String adminEmail,
                                             @Value("${arqly.initial-bootstrap.admin-password:}") String adminPassword) {
        return args -> service.bootstrap(new InitialBootstrapProperties(enabled, adminName, adminEmail, adminPassword));
    }
}
