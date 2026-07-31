package com.arqly.backend.dto;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.FileVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FileDtos {
    private FileDtos() {}

    public record FolderRequest(
            @NotNull FileOwnerType ownerType,
            @NotNull UUID ownerId,
            UUID parentId,
            @NotBlank @Size(max = 255) String name
    ) {}

    public record FolderResponse(
            UUID id, UUID parentId, String name, FileOwnerType ownerType, UUID ownerId, Instant createdAt
    ) {}

    public record FileUploadMetadata(
            @NotNull FileOwnerType ownerType,
            @NotNull UUID ownerId,
            UUID folderId,
            FileVisibility visibility,
            String conflictStrategy,
            UUID existingFileId,
            @Size(max = 1000) String revisionComment,
            List<@Size(max = 60) String> tags
    ) {}

    public record FileUpdateRequest(
            @Size(max = 255) String name,
            UUID folderId,
            FileVisibility visibility,
            List<@Size(max = 60) String> tags
    ) {}

    public record FileMoveRequest(
            @NotNull FileOwnerType ownerType,
            @NotNull UUID ownerId,
            UUID folderId
    ) {}

    public record FileResponse(
            UUID id, FileOwnerType ownerType, UUID ownerId, String ownerLabel, UUID folderId, String folderName,
            String name, String originalName, String extension, String mimeType, long size,
            String checksum, int version, FileVisibility visibility, FileResourceStatus status,
            UUID uploadedById, String uploadedByName, List<String> tags, boolean previewAvailable,
            Instant createdAt, Instant updatedAt
    ) {}

    public record FileVersionResponse(
            UUID id, int versionNumber, String checksum, long size, UUID authorId, String authorName,
            String revisionComment, Instant createdAt
    ) {}

    public record FileTagResponse(UUID id, String name) {}

    public record FileStatsResponse(long active, long archived, long deleted, long totalSize) {}
}
