package com.arqly.backend.dto;

import com.arqly.backend.entity.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TenantDtos {
    private TenantDtos() {}

    public record TenantRequest(
            @NotBlank String tradeName,
            @NotBlank String legalName,
            @NotBlank @Size(min = 14, max = 20) String cnpj,
            @Email @NotBlank String primaryEmail,
            String phone,
            String address,
            String city,
            String state,
            String zipCode,
            TenantStatus status
    ) {}

    public record TenantResponse(UUID id, String tradeName, String legalName, String cnpj, String primaryEmail,
                                 String phone, String address, String city, String state, String zipCode,
                                 TenantStatus status, Instant createdAt) {}

    public record CreateTenantAdminRequest(@NotBlank String name, @Email @NotBlank String email) {}
    public record TenantAdminResponse(UUID id, UUID tenantId, String name, String email, String firstAccessUrl) {}
}
