package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.ApprovalRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ApprovalCalendarProvider implements CalendarEventProvider {
 private final ApprovalRepository approvals; private final CalendarItemFactory factory;
 public ApprovalCalendarProvider(ApprovalRepository approvals,CalendarItemFactory factory){this.approvals=approvals;this.factory=factory;}
 public List<CalendarItemResponse> provide(UUID tenantId,LocalDate start,LocalDate end,CalendarFilters filters){
  if(filters.sourceType()!=null&&!filters.sourceType().isBlank()&&!"APPROVAL".equals(filters.sourceType()))return List.of();
  return approvals.findAllByTenantIdAndDeletedFalseAndDeadlineBetween(tenantId,start,end).stream().filter(a->filters.projectId()==null||a.getProject().getId().equals(filters.projectId())).filter(a->filters.clientId()==null||a.getClient().getId().equals(filters.clientId())).map(a->factory.allDay(a.getId(),CalendarSourceType.APPROVAL,"Aprovação — "+a.getDescription(),a.getDeadline(),"APPROVAL",a.getStatus().name(),a.getProject(),a.getStage(),a.getCreatedBy(),ActivityVisibility.INTERNAL,false,"/app/projects/"+a.getProject().getId(),"#d97706")).toList();
 }
}
