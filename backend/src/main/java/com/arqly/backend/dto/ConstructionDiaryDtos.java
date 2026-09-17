package com.arqly.backend.dto;

import com.arqly.backend.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

public final class ConstructionDiaryDtos {
    private ConstructionDiaryDtos() {}
    public record ParticipantRequest(@NotNull ConstructionDiaryParticipantType participantType, UUID tenantUserId, String name, String company, String role, String phone, @Email String email) {}
    public record ObservationRequest(@NotBlank @Size(max=255) String title, String description, @NotNull ConstructionDiaryObservationCategory category, @NotNull ConstructionDiaryObservationStatus status, int order, ActivityVisibility visibility) {}
    public record OccurrenceRequest(@NotBlank @Size(max=255) String title, String description, @NotNull ConstructionDiarySeverity severity, UUID responsibleUserId, LocalDate dueDate, boolean resolved, String resolution, ActivityVisibility visibility) {}
    public record DecisionRequest(@NotBlank String description, String decidedBy, LocalDate decisionDate, ActivityVisibility visibility) {}
    public record InstructionRequest(@NotBlank String description, String responsible, LocalDate deadline, boolean completed, ActivityVisibility visibility) {}
    public record PhotoRequest(@NotNull UUID fileId, String caption, String description, int order, ActivityVisibility visibility) {}
    public record EntryRequest(@NotNull UUID projectId, UUID responsibleUserId, @NotNull ConstructionDiaryEntryType entryType, @NotBlank @Size(max=255) String title,
                               @NotNull LocalDate entryDate, LocalTime startTime, LocalTime endTime, String location, String summary,
                               String weatherCondition, @DecimalMin("-50") @DecimalMax("70") BigDecimal temperature, ActivityVisibility visibility,
                               LocalDate nextVisitDate, String nextVisitNotes, List<UUID> stageIds,
                               List<@Valid ParticipantRequest> participants, List<@Valid ObservationRequest> observations,
                               List<@Valid OccurrenceRequest> occurrences, List<@Valid DecisionRequest> decisions, List<@Valid InstructionRequest> instructions) {}
    public record ParticipantResponse(UUID id, ConstructionDiaryParticipantType participantType, UUID tenantUserId, String name, String company, String role, String phone, String email) {}
    public record ObservationResponse(UUID id, String title, String description, ConstructionDiaryObservationCategory category, ConstructionDiaryObservationStatus status, int order, ActivityVisibility visibility) {}
    public record OccurrenceResponse(UUID id, String title, String description, ConstructionDiarySeverity severity, UUID responsibleUserId, String responsibleName, LocalDate dueDate, boolean resolved, Instant resolvedAt, String resolution, ActivityVisibility visibility) {}
    public record DecisionResponse(UUID id, String description, String decidedBy, LocalDate decisionDate, ActivityVisibility visibility) {}
    public record InstructionResponse(UUID id, String description, String responsible, LocalDate deadline, boolean completed, ActivityVisibility visibility) {}
    public record PhotoResponse(UUID id, UUID fileId, String fileName, String mimeType, long size, String caption, String description, int order, ActivityVisibility visibility) {}
    public record DiarySummaryResponse(UUID id, UUID projectId, String projectName, String title, ConstructionDiaryEntryType entryType, ConstructionDiaryStatus status,
                                       LocalDate entryDate, String responsibleName, int occurrenceCount, int photoCount, ActivityVisibility visibility, LocalDate nextVisitDate, Instant updatedAt) {}
    public record DiaryResponse(UUID id, UUID projectId, String projectName, UUID responsibleUserId, String responsibleName, ConstructionDiaryEntryType entryType,
                                ConstructionDiaryStatus status, String title, LocalDate entryDate, LocalTime startTime, LocalTime endTime, String location,
                                String summary, String weatherCondition, BigDecimal temperature, ActivityVisibility visibility, LocalDate nextVisitDate,
                                String nextVisitNotes, Instant publishedAt, String publishedByName, int revisionNumber, Instant lastRevisionAt,
                                String lastRevisionByName, List<UUID> stageIds, List<String> stageNames, List<ParticipantResponse> participants,
                                List<ObservationResponse> observations, List<OccurrenceResponse> occurrences, List<DecisionResponse> decisions,
                                List<InstructionResponse> instructions, List<PhotoResponse> photos, Instant createdAt, String createdByName) {}
    public record DiaryStatsResponse(long entries, long openOccurrences, long criticalOccurrences, LocalDate lastVisit, LocalDate nextVisit) {}
}
