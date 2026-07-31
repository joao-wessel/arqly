package com.arqly.backend.dto;

import com.arqly.backend.entity.DocumentCategory;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DocumentDtos {
    private DocumentDtos() {}

    public record DocumentTemplateRequest(
            @NotBlank String name,
            String description,
            @NotNull DocumentCategory category,
            @NotBlank String content,
            boolean active
    ) {}

    public record DocumentTemplateSummaryResponse(
            UUID id,
            UUID seriesId,
            String name,
            String description,
            DocumentCategory category,
            boolean active,
            boolean archived,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record DocumentTemplateResponse(
            UUID id,
            UUID seriesId,
            UUID previousVersionId,
            String name,
            String description,
            DocumentCategory category,
            String content,
            boolean active,
            boolean archived,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record DocumentPreviewRequest(
            @NotNull UUID templateId,
            UUID projectId,
            UUID proposalId,
            UUID clientId,
            String title
    ) {}

    public record DocumentPreviewResponse(
            UUID templateId,
            String title,
            String content,
            List<String> unresolvedVariables,
            UUID projectId,
            String projectName,
            UUID proposalId,
            String proposalNumber,
            UUID clientId,
            String clientName
    ) {}

    public record GenerateDocumentRequest(
            @NotNull UUID templateId,
            UUID projectId,
            UUID proposalId,
            UUID clientId,
            @NotBlank String title,
            @NotBlank String content,
            GeneratedDocumentStatus status
    ) {}

    public record NewDocumentVersionRequest(
            @NotBlank String title,
            @NotBlank String content,
            GeneratedDocumentStatus status
    ) {}

    public record GeneratedDocumentSummaryResponse(
            UUID id,
            UUID seriesId,
            UUID templateId,
            String templateName,
            DocumentCategory category,
            UUID projectId,
            String projectName,
            UUID proposalId,
            String proposalNumber,
            UUID clientId,
            String clientName,
            String title,
            int version,
            GeneratedDocumentStatus status,
            UUID generatedById,
            String generatedByName,
            boolean clientVisible,
            Instant generatedAt
    ) {}

    public record GeneratedDocumentResponse(
            UUID id,
            UUID seriesId,
            UUID previousVersionId,
            UUID templateId,
            String templateName,
            DocumentCategory category,
            UUID projectId,
            String projectName,
            UUID proposalId,
            String proposalNumber,
            UUID clientId,
            String clientName,
            String title,
            String content,
            int version,
            GeneratedDocumentStatus status,
            UUID generatedById,
            String generatedByName,
            boolean clientVisible,
            Instant generatedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record DocumentVariableResponse(
            String group,
            String label,
            String placeholder,
            String description
    ) {}
}
