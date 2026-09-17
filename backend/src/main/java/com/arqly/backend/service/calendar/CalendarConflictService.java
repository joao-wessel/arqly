package com.arqly.backend.service.calendar;

import com.arqly.backend.repository.CalendarEventRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CalendarConflictService {
 private final CalendarEventRepository events; public CalendarConflictService(CalendarEventRepository events){this.events=events;}
 public boolean hasConflict(UUID tenantId, UUID responsibleUserId, OffsetDateTime start, OffsetDateTime end, UUID ignoredId){
  if(responsibleUserId==null)return false; return events.findForInterval(tenantId,start,end==null?start.plusHours(1):end).stream().anyMatch(e->!e.getId().equals(ignoredId)&&e.getResponsibleUser()!=null&&e.getResponsibleUser().getId().equals(responsibleUserId));
 }
}
