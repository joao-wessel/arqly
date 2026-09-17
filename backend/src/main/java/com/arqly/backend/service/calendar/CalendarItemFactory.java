package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CalendarItemFactory {
    public CalendarItemResponse allDay(UUID id, CalendarSourceType source, String title, LocalDate date, String type, String status,
        Project project, ProjectStage stage, TenantUser responsible, ActivityVisibility visibility, boolean editable, String sourceUrl, String color) {
        return new CalendarItemResponse(id, source, id, title, date.toString(), date.plusDays(1).toString(), true, type, status,
            project == null ? null : project.getId(), project == null ? null : project.getName(),
            project == null ? null : project.getClient().getId(), project == null ? null : project.getClient().getName(),
            responsible == null ? null : responsible.getId(), responsible == null ? null : responsible.getName(), visibility,
            editable, editable, sourceUrl, color);
    }
}
