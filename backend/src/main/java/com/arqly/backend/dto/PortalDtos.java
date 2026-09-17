package com.arqly.backend.dto;

import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ApprovalStatus;
import com.arqly.backend.entity.DocumentCategory;
import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileVisibility;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.entity.ProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PortalDtos {
    private PortalDtos() {}

    public record PortalDashboardResponse(
            PortalClientResponse client,
            long projectCount,
            long activeProjects,
            long completedProjects,
            long pendingProposals,
            long documentCount,
            long pendingApprovals,
            Instant lastUpdate,
            List<PortalActivityResponse> latestActivities
    ) {}

    public record PortalClientResponse(UUID id, String displayName, String email, String phone, String city, String state) {}

    public record PortalProjectSummaryResponse(
            UUID id,
            String code,
            String name,
            ProjectStatus status,
            BigDecimal progressPercentage,
            String responsibleName,
            Instant updatedAt
    ) {}

    public record PortalProjectResponse(
            UUID id,
            String code,
            String name,
            String description,
            ProjectStatus status,
            BigDecimal progressPercentage,
            String responsibleName,
            String clientName,
            String proposalNumber,
            LocalDate startDate,
            LocalDate expectedEndDate,
            List<PortalPhaseResponse> phases,
            List<PortalDocumentResponse> documents,
            List<PortalFileResponse> files,
            List<PortalApprovalResponse> approvals,
            List<PortalActivityResponse> activities
    ) {}

    public record PortalPhaseResponse(UUID id, String name, String color, String icon, ProjectStageStatus status,
                                      BigDecimal completionPercentage, List<PortalStageResponse> stages) {}

    public record PortalStageResponse(UUID id, String name, ProjectStageStatus status, BigDecimal completionPercentage,
                                      String responsibleName, LocalDate plannedEnd) {}

    public record PortalProposalSummaryResponse(UUID id, String number, String title, ProposalStatus status,
                                                BigDecimal total, LocalDate validUntil, String portalUrl, Instant createdAt) {}

    public record PortalDocumentResponse(UUID id, String title, DocumentCategory category, int version,
                                         GeneratedDocumentStatus status, Instant generatedAt) {}

    public record PortalFileResponse(UUID id, FileOwnerType ownerType, UUID ownerId, String name, String extension,
                                     String mimeType, long size, int version, FileVisibility visibility,
                                     boolean previewAvailable, Instant updatedAt) {}

    public record PortalActivityResponse(UUID id, UUID projectId, UUID stageId, String authorName, String authorRole,
                                         ActivityType type, String title, String description, Instant createdAt) {}

    public record PortalApprovalResponse(UUID id, UUID projectId, String projectName, UUID stageId, String stageName,
                                         UUID documentId, String documentTitle, ApprovalStatus status, String description,
                                         LocalDate deadline, Instant approvedAt, String clientComment, Instant createdAt) {}

    public record PortalDiaryResponse(UUID id, UUID projectId, String projectName, String title, String entryType,
                                      LocalDate entryDate, String responsibleName, String summary, String location,
                                      List<String> stages, List<PortalDiaryObservationResponse> observations,
                                      List<PortalDiaryOccurrenceResponse> occurrences, List<PortalDiaryDecisionResponse> decisions,
                                      List<PortalDiaryPhotoResponse> photos, LocalDate nextVisitDate, Instant publishedAt) {}
    public record PortalDiaryObservationResponse(String title, String description, String category, String status) {}
    public record PortalDiaryOccurrenceResponse(String title, String description, String severity, boolean resolved, LocalDate dueDate) {}
    public record PortalDiaryDecisionResponse(String description, String decidedBy, LocalDate decisionDate) {}
    public record PortalDiaryPhotoResponse(UUID fileId, String fileName, String mimeType, String caption, String description) {}

    public record PortalProfileRequest(String phone, String language, String themeMode, String colorPalette) {}
}
