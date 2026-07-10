package com.arqly.backend.dto;

import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record CreateProjectFromProposalRequest(
            @NotBlank String name,
            UUID templateId,
            LocalDate startDate,
            LocalDate expectedEndDate,
            String responsibleArchitect,
            String internalNotes
    ) {}

    public record ProjectUpdateRequest(
            @NotBlank String name,
            String description,
            ProjectStatus status,
            UUID templateId,
            LocalDate startDate,
            LocalDate expectedEndDate,
            LocalDate completedAt,
            String responsibleArchitect,
            String internalNotes
    ) {}

    public record ProjectSummaryResponse(
            UUID id,
            String code,
            String name,
            UUID clientId,
            String clientName,
            UUID proposalId,
            String proposalNumber,
            UUID templateId,
            String templateName,
            String responsibleArchitect,
            ProjectStatus status,
            LocalDate expectedEndDate,
            BigDecimal contractedValue,
            BigDecimal progressPercentage,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectResponse(
            UUID id,
            String code,
            String name,
            String description,
            UUID clientId,
            String clientName,
            UUID proposalId,
            String proposalNumber,
            UUID templateId,
            String templateName,
            String responsibleArchitect,
            ProjectStatus status,
            LocalDate startDate,
            LocalDate expectedEndDate,
            LocalDate completedAt,
            BigDecimal contractedValue,
            BigDecimal progressPercentage,
            String internalNotes,
            String createdBy,
            String updatedBy,
            List<ProjectServiceResponse> services,
            List<ProjectStageResponse> stages,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectServiceResponse(
            UUID id,
            String name,
            String description,
            BigDecimal quantity,
            BillingUnit unit,
            BigDecimal contractedValue
    ) {}

    public record ProjectStatsResponse(
            long active,
            long completed,
            long paused,
            long cancelled,
            long createdThisMonth,
            long delayedStages,
            long completedStages,
            long activeStages
    ) {}

    public record ProjectTemplateRequest(
            @NotBlank String name,
            String description,
            boolean active
    ) {}

    public record StageChecklistRequest(
            @NotBlank String title,
            int order,
            boolean completed
    ) {}

    public record StageTemplateRequest(
            @NotBlank String name,
            String description,
            int order,
            String color,
            String icon,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weightPercentage,
            boolean active,
            @Valid List<StageChecklistRequest> checklist
    ) {}

    public record StageTemplateResponse(
            UUID id,
            String name,
            String description,
            int order,
            String color,
            String icon,
            BigDecimal weightPercentage,
            boolean active,
            List<StageChecklistResponse> checklist,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectStageRequest(
            @NotBlank String name,
            String description,
            int order,
            String color,
            String icon,
            ProjectStageStatus status,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal completionPercentage,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weightPercentage,
            String responsible,
            String notes,
            UUID dependsOnStageId,
            @Valid List<StageChecklistRequest> checklist
    ) {}

    public record ProjectStageResponse(
            UUID id,
            UUID templateId,
            UUID dependsOnStageId,
            String name,
            String description,
            int order,
            String color,
            String icon,
            ProjectStageStatus status,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            BigDecimal completionPercentage,
            BigDecimal weightPercentage,
            String responsible,
            String notes,
            List<StageChecklistResponse> checklist,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record StageChecklistResponse(
            UUID id,
            String title,
            int order,
            boolean completed,
            Instant completedAt
    ) {}

    public record ReorderItemRequest(
            UUID id,
            int order
    ) {}

    public record ReorderRequest(
            @NotEmpty List<ReorderItemRequest> items
    ) {}

    public record ProjectTemplateResponse(
            UUID id,
            String name,
            String description,
            boolean active,
            long stageCount,
            long projectsUsing,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
