package com.arqly.backend.service;

import com.arqly.backend.config.InitialBootstrapProperties;
import com.arqly.backend.entity.PlatformUser;
import com.arqly.backend.entity.Role;
import com.arqly.backend.repository.PlatformUserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialPlatformBootstrapService {
    private static final Logger log = LoggerFactory.getLogger(InitialPlatformBootstrapService.class);
    private final PlatformUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public InitialPlatformBootstrapService(PlatformUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void bootstrap(InitialBootstrapProperties properties) {
        if (!properties.enabled()) return;
        require(properties.adminName(), "ARQLY_INITIAL_ADMIN_NAME");
        require(properties.adminEmail(), "ARQLY_INITIAL_ADMIN_EMAIL");
        require(properties.adminPassword(), "ARQLY_INITIAL_ADMIN_PASSWORD");
        if (repository.existsByEmailIgnoreCase(properties.adminEmail().trim())) return;

        var user = new PlatformUser();
        user.setName(properties.adminName().trim());
        user.setEmail(properties.adminEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        user.setActive(true);
        user.setRoles(Set.of(Role.ROLE_PLATFORM_ADMIN));
        repository.save(user);
        log.info("Initial platform admin created");
    }

    private void require(String value, String property) {
        if (value == null || value.isBlank()) throw new IllegalStateException(property + " must be set when initial bootstrap is enabled");
    }
}
