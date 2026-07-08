package com.arqly.backend.config;

import com.arqly.backend.entity.PlatformUser;
import com.arqly.backend.repository.PlatformUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfig {
    @Bean
    CommandLineRunner bootstrapPlatformAdmin(PlatformUserRepository repository, PasswordEncoder encoder,
                                             @Value("${arqly.bootstrap.platform-email}") String email,
                                             @Value("${arqly.bootstrap.platform-password}") String password) {
        return args -> {
            if (!repository.existsByEmailIgnoreCase(email)) {
                var user = new PlatformUser();
                user.setName("Administrador Arqly");
                user.setEmail(email);
                user.setPasswordHash(encoder.encode(password));
                repository.save(user);
            }
        };
    }
}
