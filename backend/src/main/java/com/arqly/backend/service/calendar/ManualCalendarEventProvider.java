package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.CalendarEventRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ManualCalendarEventProvider implements CalendarEventProvider {
 private final CalendarEventRepository events;
 public ManualCalendarEventProvider(CalendarEventRepository events){this.events=events;}
 public List<CalendarItemResponse> provide(UUID tenantId,LocalDate start,LocalDate end,CalendarFilters filters){
  if(filters.sourceType()!=null&&!filters.sourceType().isBlank()&&!"MANUAL_EVENT".equals(filters.sourceType()))return List.of();
  var from=start.atStartOfDay().atOffset(ZoneOffset.UTC);var to=end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
  return events.findForInterval(tenantId,from,to).stream().filter(e->accept(e,filters)).map(e->new CalendarItemResponse(e.getId(),CalendarSourceType.MANUAL_EVENT,e.getId(),e.getTitle(),format(e.getStartDateTime(),e.isAllDay()),e.getEndDateTime()==null?null:format(e.getEndDateTime(),e.isAllDay()),e.isAllDay(),e.getType().name(),e.getStatus().name(),e.getProject()==null?null:e.getProject().getId(),e.getProject()==null?null:e.getProject().getName(),e.getClient()==null?null:e.getClient().getId(),e.getClient()==null?null:e.getClient().getName(),e.getResponsibleUser()==null?null:e.getResponsibleUser().getId(),e.getResponsibleUser()==null?null:e.getResponsibleUser().getName(),e.getVisibility(),true,true,"/app/calendar?event="+e.getId(),color(e.getType()))).toList();
 }
 private boolean accept(CalendarEvent e,CalendarFilters f){return (f.projectId()==null||(e.getProject()!=null&&e.getProject().getId().equals(f.projectId())))&&(f.clientId()==null||(e.getClient()!=null&&e.getClient().getId().equals(f.clientId())))&&(f.responsibleUserId()==null||(e.getResponsibleUser()!=null&&e.getResponsibleUser().getId().equals(f.responsibleUserId())))&&(f.status()==null||f.status().isBlank()||e.getStatus().name().equals(f.status()))&&(f.search()==null||f.search().isBlank()||e.getTitle().toLowerCase().contains(f.search().toLowerCase()));}
 private String format(OffsetDateTime value,boolean allDay){return allDay?value.toLocalDate().toString():value.toString();}
 private String color(CalendarEventType type){return switch(type){case SITE_VISIT,INSPECTION->"#0f766e";case DELIVERY->"#2563eb";case CALL->"#7c3aed";case MEETING,PRESENTATION->"#d97706";default->"var(--arqly-600)";};}
}
