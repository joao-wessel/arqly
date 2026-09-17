package com.arqly.backend.dto;

import com.arqly.backend.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CalendarDtos {
    private CalendarDtos() {}
    public record CalendarItemResponse(UUID id, CalendarSourceType sourceType, UUID sourceId, String title,
        String start, String end, boolean allDay, String type, String status, UUID projectId, String projectName,
        UUID clientId, String clientName, UUID responsibleUserId, String responsibleUserName,
        ActivityVisibility visibility, boolean editable, boolean deletable, String sourceUrl, String color) {}
    public record CalendarEventRequest(@NotBlank String title, String description, @NotNull CalendarEventType type,
        @NotNull OffsetDateTime startDateTime, OffsetDateTime endDateTime, boolean allDay, String location,
        UUID projectId, UUID clientId, UUID stageId, UUID responsibleUserId, ActivityVisibility visibility) {}
    public record CalendarEventResponse(UUID id, String title, String description, CalendarEventType type,
        OffsetDateTime startDateTime, OffsetDateTime endDateTime, boolean allDay, String location, UUID projectId,
        UUID clientId, UUID stageId, UUID responsibleUserId, String responsibleUserName, ActivityVisibility visibility,
        CalendarEventStatus status, boolean deleted) {}
}
