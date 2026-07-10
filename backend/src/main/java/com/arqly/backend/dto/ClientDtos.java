package com.arqly.backend.dto;

import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.ClientStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ClientDtos {
    private ClientDtos() {}

    public record ClientRequest(
            @NotNull ClientPersonType personType,
            String name,
            @Size(max = 20) String cpf,
            String rg,
            LocalDate birthDate,
            String legalName,
            String tradeName,
            @Size(max = 20) String cnpj,
            String stateRegistration,
            @Email @NotBlank String email,
            String phone,
            String whatsapp,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            @Size(min = 2, max = 2) String state,
            String notes,
            @NotNull ClientStatus status
    ) {
        @AssertTrue(message = "Nome e CPF são obrigatórios para pessoa física.")
        public boolean isNaturalPersonValid() {
            return personType != ClientPersonType.NATURAL_PERSON || filled(name) && filled(cpf);
        }

        @AssertTrue(message = "Razão social, nome fantasia e CNPJ são obrigatórios para pessoa jurídica.")
        public boolean isLegalEntityValid() {
            return personType != ClientPersonType.LEGAL_ENTITY || filled(legalName) && filled(tradeName) && filled(cnpj);
        }

        private boolean filled(String value) {
            return value != null && !value.isBlank();
        }
    }

    public record ClientSummaryResponse(
            UUID id,
            ClientPersonType personType,
            String displayName,
            String document,
            String email,
            String phone,
            String city,
            String state,
            ClientStatus status,
            boolean portalActive,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ClientResponse(
            UUID id,
            ClientPersonType personType,
            String name,
            String cpf,
            String rg,
            LocalDate birthDate,
            String legalName,
            String tradeName,
            String cnpj,
            String stateRegistration,
            String email,
            String phone,
            String whatsapp,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state,
            String notes,
            ClientStatus status,
            Instant createdAt,
            Instant updatedAt,
            ClientPortalAccessResponse portalAccess
    ) {}

    public record ClientPortalAccessResponse(
            UUID id,
            UUID token,
            String portalUrl,
            Instant createdAt,
            Instant expiresAt,
            boolean revoked,
            Instant lastAccessAt,
            boolean active
    ) {}

    public record PortalValidityRequest(@NotNull Instant expiresAt) {}

    public record PortalPublicResponse(ClientPublicResponse client, List<Object> projects, String message) {}

    public record ClientPublicResponse(UUID id, String displayName, String email, String phone, String city, String state) {}
}
