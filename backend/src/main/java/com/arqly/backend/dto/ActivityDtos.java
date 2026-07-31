package com.arqly.backend.dto;

import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ActivityVisibility;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class ActivityDtos {
    private ActivityDtos() {}

    public record CommentActivityRequest(@NotBlank String comment) {}

    public record ActivityResponse(
            UUID id,
            UUID projectId,
            UUID clientId,
            UUID proposalId,
            UUID generatedDocumentId,
            UUID phaseId,
            UUID stageId,
            UUID authorId,
            String authorName,
            String authorRole,
            ActivityType type,
            ActivityVisibility visibility,
            String title,
            String description,
            String metadata,
            boolean edited,
            boolean deleted,
            Instant editedAt,
            Instant createdAt
    ) {}
}
