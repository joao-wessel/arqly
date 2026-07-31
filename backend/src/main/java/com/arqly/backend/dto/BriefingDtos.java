package com.arqly.backend.dto;

import com.arqly.backend.entity.BriefingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BriefingDtos {
    private BriefingDtos() {}

    public record BriefingRequirementRequest(
            @NotBlank String description,
            int order
    ) {}

    public record BriefingRequest(
            @NotNull UUID clientId,
            UUID projectTemplateId,
            UUID responsibleUserId,
            @NotBlank String title,
            String description,
            BriefingStatus status,
            @DecimalMin("0.00") BigDecimal approximateArea,
            String workAddress,
            String city,
            String state,
            LocalDate desiredDeadline,
            @DecimalMin("0.00") BigDecimal expectedBudget,
            String architecturalStyle,
            String colorPalette,
            String desiredMaterials,
            String preferenceNotes,
            String legalRestrictions,
            String technicalRestrictions,
            String clientRestrictions,
            String restrictionNotes,
            @Valid List<BriefingRequirementRequest> requirements
    ) {}

    public record BriefingRequirementResponse(
            UUID id,
            String description,
            int order,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record BriefingSummaryResponse(
            UUID id,
            UUID clientId,
            String clientName,
            UUID projectTemplateId,
            String projectTemplateName,
            UUID responsibleUserId,
            String responsibleName,
            String title,
            BriefingStatus status,
            boolean proposalGenerated,
            UUID proposalId,
            String proposalNumber,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record BriefingResponse(
            UUID id,
            UUID clientId,
            String clientName,
            UUID projectTemplateId,
            String projectTemplateName,
            UUID responsibleUserId,
            String responsibleName,
            String title,
            String description,
            BriefingStatus status,
            BigDecimal approximateArea,
            String workAddress,
            String city,
            String state,
            LocalDate desiredDeadline,
            BigDecimal expectedBudget,
            String architecturalStyle,
            String colorPalette,
            String desiredMaterials,
            String preferenceNotes,
            String legalRestrictions,
            String technicalRestrictions,
            String clientRestrictions,
            String restrictionNotes,
            boolean proposalGenerated,
            UUID proposalId,
            String proposalNumber,
            List<BriefingRequirementResponse> requirements,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record BriefingStatsResponse(
            long total,
            long inProgress,
            long completed,
            long converted
    ) {}
}
