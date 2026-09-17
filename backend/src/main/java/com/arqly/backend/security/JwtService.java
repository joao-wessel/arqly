package com.arqly.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecurityProperties properties;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
    }

    public String createPlatformToken(UUID userId, String email, Set<String> roles) {
        return create(userId, null, email, roles, key(properties.platformSecret()));
    }

    public String createTenantToken(UUID userId, UUID tenantId, String email, Set<String> roles) {
        return create(userId, tenantId, email, roles, key(properties.tenantSecret()));
    }

    public Claims parsePlatform(String token) {
        return parse(token, key(properties.platformSecret()));
    }

    public Claims parseTenant(String token) {
        return parse(token, key(properties.tenantSecret()));
    }

    private String create(UUID userId, UUID tenantId, String email, Set<String> roles, SecretKey key) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.accessTokenMinutes() * 60)))
                .claim("uid", userId.toString())
                .claim("roles", roles);
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }
        return builder.signWith(key).compact();
    }

    private Claims parse(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).requireIssuer(properties.issuer()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
