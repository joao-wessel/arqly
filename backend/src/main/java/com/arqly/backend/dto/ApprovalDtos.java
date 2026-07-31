package com.arqly.backend.dto;

import com.arqly.backend.entity.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ApprovalDtos {
    private ApprovalDtos() {}

    public record ApprovalRequest(
            @NotNull UUID projectId,
            UUID stageId,
            UUID documentId,
            @NotBlank String description,
            LocalDate deadline
    ) {}

    public record ApprovalDecisionRequest(String comment) {}

    public record ApprovalResponse(
            UUID id,
            UUID projectId,
            String projectName,
            UUID stageId,
            String stageName,
            UUID documentId,
            String documentTitle,
            UUID clientId,
            String clientName,
            ApprovalStatus status,
            String description,
            LocalDate deadline,
            Instant approvedAt,
            String approvedBy,
            String createdByName,
            String clientComment,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
