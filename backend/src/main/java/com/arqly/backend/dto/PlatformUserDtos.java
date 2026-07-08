package com.arqly.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class PlatformUserDtos {
    private PlatformUserDtos() {}

    public record PlatformUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            String password,
            boolean active
    ) {}

    public record PlatformUserResponse(
            UUID id,
            String name,
            String email,
            boolean active,
            Set<String> roles,
            Instant lastAccessAt,
            Instant createdAt
    ) {}
}
