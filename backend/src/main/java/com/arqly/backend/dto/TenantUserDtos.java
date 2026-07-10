package com.arqly.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class TenantUserDtos {
    private TenantUserDtos() {}

    public record TenantUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            String password,
            boolean active,
            boolean tenantAdmin
    ) {}

    public record TenantUserResponse(
            UUID id,
            String name,
            String email,
            boolean active,
            boolean tenantAdmin,
            Set<String> roles,
            Instant lastAccessAt,
            Instant createdAt
    ) {}
}
