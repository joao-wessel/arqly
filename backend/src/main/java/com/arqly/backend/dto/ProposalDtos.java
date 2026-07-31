package com.arqly.backend.dto;

import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.entity.OriginType;
import com.arqly.backend.entity.ProposalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProposalDtos {
    private ProposalDtos() {}

    public record ProposalRequest(
            @NotNull UUID clientId,
            UUID briefingId,
            @NotBlank String title,
            String description,
            LocalDate validUntil,
            @DecimalMin("0.00") BigDecimal discount,
            @DecimalMin("0.00") BigDecimal addition,
            String scope,
            String exclusions,
            String internalNotes,
            String clientNotes,
            @NotEmpty @Valid List<ProposalItemRequest> items,
            @Valid List<ProposalPaymentConditionRequest> paymentConditions
    ) {}

    public record ProposalItemRequest(
            @NotNull UUID serviceId,
            String customDescription,
            @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
            BillingUnit unit,
            @DecimalMin("0.00") BigDecimal unitValue,
            @DecimalMin("0.00") BigDecimal discount
    ) {}

    public record ProposalPaymentConditionRequest(
            @NotBlank String description,
            @DecimalMin("0.00") BigDecimal percentage,
            @DecimalMin("0.00") BigDecimal value,
            LocalDate dueDate
    ) {}

    public record SendProposalRequest(
            @Size(max = 1000) String message
    ) {}

    public record ProposalDecisionRequest(
            @Size(max = 1000) String note
    ) {}

    public record ProposalSummaryResponse(
            UUID id,
            String number,
            UUID clientId,
            String clientName,
            UUID briefingId,
            String briefingTitle,
            OriginType originType,
            String title,
            BigDecimal total,
            ProposalStatus status,
            LocalDate validUntil,
            String createdBy,
            boolean projectCreated,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProposalResponse(
            UUID id,
            String number,
            UUID clientId,
            String clientName,
            String clientEmail,
            UUID briefingId,
            String briefingTitle,
            OriginType originType,
            String title,
            String description,
            LocalDate validUntil,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal addition,
            BigDecimal total,
            ProposalStatus status,
            String scope,
            String exclusions,
            String internalNotes,
            String clientNotes,
            String createdBy,
            String updatedBy,
            Instant sentAt,
            Instant viewedAt,
            Instant acceptedAt,
            Instant rejectedAt,
            boolean projectCreated,
            List<ProposalItemResponse> items,
            List<ProposalPaymentConditionResponse> paymentConditions,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProposalItemResponse(
            UUID id,
            UUID serviceId,
            String serviceName,
            String serviceDescription,
            String customDescription,
            BigDecimal quantity,
            BillingUnit unit,
            BigDecimal unitValue,
            BigDecimal discount,
            BigDecimal total
    ) {}

    public record ProposalPaymentConditionResponse(
            UUID id,
            String description,
            BigDecimal percentage,
            BigDecimal value,
            LocalDate dueDate
    ) {}

    public record ProposalStatsResponse(
            long quantity,
            BigDecimal totalValue,
            long accepted,
            long rejected,
            long expired,
            long pending,
            long manual,
            long fromBriefings
    ) {}

    public record PortalProposalResponse(
            ProposalPublicClient client,
            String message,
            ProposalResponse proposal
    ) {}

    public record ProposalPublicClient(
            UUID id,
            String displayName,
            String email,
            String phone
    ) {}

    public record ProjectCreatedResponse(
            UUID projectId,
            UUID proposalId,
            String title,
            String message
    ) {}
}
