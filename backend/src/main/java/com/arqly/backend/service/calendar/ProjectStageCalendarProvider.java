package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.ProjectStageRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ProjectStageCalendarProvider implements CalendarEventProvider {
    private final ProjectStageRepository stages; private final CalendarItemFactory factory;
    public ProjectStageCalendarProvider(ProjectStageRepository stages, CalendarItemFactory factory){this.stages=stages;this.factory=factory;}
    public List<CalendarItemResponse> provide(UUID tenantId, LocalDate start, LocalDate end, CalendarFilters filters) {
        if (filters.sourceType()!=null && !filters.sourceType().isBlank() && !"PROJECT_STAGE".equals(filters.sourceType())) return List.of();
        return stages.findDueForCalendar(tenantId,start,end).stream()
            .filter(s->filters.projectId()==null || s.getProjectPhase().getProject().getId().equals(filters.projectId()))
            .filter(s->filters.clientId()==null || s.getProjectPhase().getProject().getClient().getId().equals(filters.clientId()))
            .filter(s->filters.responsibleUserId()==null || (s.getResponsibleUser()!=null && s.getResponsibleUser().getId().equals(filters.responsibleUserId())))
            .map(s->factory.allDay(s.getId(), CalendarSourceType.PROJECT_STAGE, "Prazo — "+s.getName(), s.getPlannedEnd(), "STAGE", s.getStatus().name(), s.getProjectPhase().getProject(), s, s.getResponsibleUser(), ActivityVisibility.INTERNAL, false, "/app/projects/"+s.getProjectPhase().getProject().getId()+"/stages/"+s.getId(), "var(--arqly-600)")) .toList();
    }
}
