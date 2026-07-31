package com.arqly.backend.dto;

import com.arqly.backend.entity.ProgressCalculationMode;
import com.arqly.backend.entity.ProjectStageStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StageWorkspaceDtos {
    private StageWorkspaceDtos() {}

    public record StageWorkspaceResponse(
            UUID id,
            String name,
            String description,
            ProjectStageStatus status,
            BigDecimal completionPercentage,
            BigDecimal weightPercentage,
            ProgressCalculationMode progressCalculationMode,
            UUID responsibleUserId,
            String responsibleName,
            String responsible,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            long remainingDays,
            String notes,
            ProjectInfo project,
            PhaseInfo phase,
            List<ChecklistItemResponse> checklist,
            List<CommentResponse> comments,
            List<ActivityDtos.ActivityResponse> activities
    ) {}

    public record ProjectInfo(UUID id, String code, String name, String clientName, UUID proposalId, String proposalNumber) {}
    public record PhaseInfo(UUID id, String name, String color, String icon, BigDecimal completionPercentage) {}

    public record StageUpdateRequest(
            @NotBlank String name,
            String description,
            ProjectStageStatus status,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal completionPercentage,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weightPercentage,
            UUID responsibleUserId,
            String responsible,
            LocalDate plannedStart,
            LocalDate plannedEnd,
            LocalDate actualStart,
            LocalDate actualEnd,
            String notes,
            ProgressCalculationMode progressCalculationMode
    ) {}

    public record ChecklistItemRequest(
            @NotBlank String description,
            int order,
            String notes
    ) {}

    public record ChecklistToggleRequest(@NotNull Boolean completed) {}
    public record ReorderChecklistItemRequest(UUID id, int order) {}
    public record ReorderChecklistRequest(List<ReorderChecklistItemRequest> items) {}

    public record ChecklistItemResponse(
            UUID id,
            String description,
            boolean completed,
            int order,
            Instant completedAt,
            String completedBy,
            String notes
    ) {}

    public record CommentRequest(@NotBlank String comment) {}

    public record CommentResponse(
            UUID id,
            String authorId,
            String authorName,
            String comment,
            boolean edited,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record StageWorkspaceStatsResponse(
            long activeStages,
            long completedStages,
            long waitingClientStages,
            long pendingChecklistItems
    ) {}
}
