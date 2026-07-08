package com.arqly.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arqly.security")
public record SecurityProperties(String platformSecret, String tenantSecret, String issuer, long accessTokenMinutes) {
}
