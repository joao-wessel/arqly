package com.arqly.backend.config;

public record InitialBootstrapProperties(
        boolean enabled,
        String adminName,
        String adminEmail,
        String adminPassword
) {}
