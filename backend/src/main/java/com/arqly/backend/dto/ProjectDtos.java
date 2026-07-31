package com.arqly.backend.dto;

import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.entity.OriginType;
import com.arqly.backend.entity.ProgressCalculationMode;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
            UUID responsibleUserId,
            UUID projectManagerId,
            String responsibleArchitect,
            String internalNotes
    ) {}

    public record CreateProjectManualRequest(
            @NotBlank String name,
            @NotNull UUID clientId,
            UUID templateId,
            String description,
            LocalDate startDate,
            LocalDate expectedEndDate,
            UUID responsibleUserId,
            UUID projectManagerId,
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
            UUID responsibleUserId,
            UUID projectManagerId,
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
            OriginType originType,
            UUID templateId,
            String templateName,
            UUID responsibleUserId,
            String responsibleName,
            UUID projectManagerId,
            String projectManagerName,
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
            OriginType originType,
            UUID templateId,
            String templateName,
            UUID responsibleUserId,
            String responsibleName,
            UUID projectManagerId,
            String projectManagerName,
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
            List<ProjectPhaseResponse> phases,
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
            long activeStages,
            List<ResponsibleWorkloadResponse> projectsByResponsible,
            List<ResponsibleWorkloadResponse> stagesByResponsible,
            long manualProjects,
            long proposalProjects
    ) {}

    public record ResponsibleWorkloadResponse(
            UUID responsibleUserId,
            String responsibleName,
            long quantity
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

    public record PhaseTemplateRequest(
            @NotBlank String name,
            String description,
            int order,
            String color,
            String icon,
            boolean active
    ) {}

    public record PhaseTemplateResponse(
            UUID id,
            String name,
            String description,
            int order,
            String color,
            String icon,
            boolean active,
            long stageCount,
            List<StageTemplateResponse> stages,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record StageTemplateRequest(
            @NotBlank String name,
            String description,
            int order,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weightPercentage,
            ProgressCalculationMode progressCalculationMode,
            boolean active,
            @Valid List<StageChecklistRequest> checklist
    ) {}

    public record StageTemplateResponse(
            UUID id,
            UUID phaseTemplateId,
            String name,
            String description,
            int order,
            BigDecimal weightPercentage,
            ProgressCalculationMode progressCalculationMode,
            boolean active,
            List<StageChecklistResponse> checklist,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectPhaseRequest(
            @NotBlank String name,
            String description,
            int order,
            String color,
            String icon,
            ProjectStageStatus status,
            String notes
    ) {}

    public record ProjectPhaseResponse(
            UUID id,
            UUID phaseTemplateId,
            String name,
            String description,
            int order,
            String color,
            String icon,
            ProjectStageStatus status,
            BigDecimal completionPercentage,
            String notes,
            List<ProjectStageResponse> stages,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectStageRequest(
            @NotBlank String name,
            UUID projectPhaseId,
            String description,
            int order,
            ProjectStageStatus status,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal completionPercentage,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weightPercentage,
            ProgressCalculationMode progressCalculationMode,
            UUID responsibleUserId,
            String responsible,
            String notes,
            UUID dependsOnStageId,
            @Valid List<StageChecklistRequest> checklist
    ) {}

    public record ProjectStageResponse(
            UUID id,
            UUID projectPhaseId,
            UUID templateId,
            UUID dependsOnStageId,
            String name,
            String description,
            int order,
            ProjectStageStatus status,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            BigDecimal completionPercentage,
            BigDecimal weightPercentage,
            ProgressCalculationMode progressCalculationMode,
            UUID responsibleUserId,
            String responsibleName,
            String responsible,
            String notes,
            long fileCount,
            long timelineEventCount,
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
            int order,
            UUID projectPhaseId
    ) {}

    public record ReorderRequest(
            @NotEmpty List<ReorderItemRequest> items
    ) {}

    public record ProjectTemplateResponse(
            UUID id,
            String name,
            String description,
            boolean active,
            long phaseCount,
            long stageCount,
            long projectsUsing,
            List<PhaseTemplateResponse> phases,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
