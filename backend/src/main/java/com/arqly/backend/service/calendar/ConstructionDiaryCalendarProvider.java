package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ConstructionDiaryCalendarProvider implements CalendarEventProvider {
 private final ConstructionDiaryEntryRepository entries; private final ConstructionDiaryOccurrenceRepository occurrences; private final ConstructionDiaryInstructionRepository instructions; private final CalendarItemFactory factory;
 public ConstructionDiaryCalendarProvider(ConstructionDiaryEntryRepository entries,ConstructionDiaryOccurrenceRepository occurrences,ConstructionDiaryInstructionRepository instructions,CalendarItemFactory factory){this.entries=entries;this.occurrences=occurrences;this.instructions=instructions;this.factory=factory;}
 public List<CalendarItemResponse> provide(UUID tenantId,LocalDate start,LocalDate end,CalendarFilters filters){
  List<CalendarItemResponse> result=new ArrayList<>(); String source=filters.sourceType();
  if(source==null||source.isBlank()||"DIARY_VISIT".equals(source)) entries.findAllByTenantIdAndDeletedFalseAndNextVisitDateBetween(tenantId,start,end).stream().filter(e->accept(e.getProject(),filters)).forEach(e->result.add(factory.allDay(e.getId(),CalendarSourceType.DIARY_VISIT,"Próxima visita — "+e.getTitle(),e.getNextVisitDate(),"VISIT",e.getStatus().name(),e.getProject(),null,e.getResponsibleUser(),e.getVisibility(),false,"/app/construction-diary/"+e.getId(),"#0f766e")));
  if(source==null||source.isBlank()||"DIARY_OCCURRENCE".equals(source)) occurrences.findDueForCalendar(tenantId,start,end).stream().filter(o->accept(o.getDiaryEntry().getProject(),filters)).forEach(o->result.add(factory.allDay(o.getId(),CalendarSourceType.DIARY_OCCURRENCE,"Ocorrência — "+o.getTitle(),o.getDueDate(),"OCCURRENCE",o.getSeverity().name(),o.getDiaryEntry().getProject(),null,o.getResponsibleUser(),o.getVisibility(),false,"/app/construction-diary/"+o.getDiaryEntry().getId(),"#dc2626")));
  if(source==null||source.isBlank()||"DIARY_INSTRUCTION".equals(source)) instructions.findDueForCalendar(tenantId,start,end).stream().filter(i->accept(i.getDiaryEntry().getProject(),filters)).forEach(i->result.add(factory.allDay(i.getId(),CalendarSourceType.DIARY_INSTRUCTION,"Orientação — "+i.getDescription(),i.getDeadline(),"INSTRUCTION","PENDING",i.getDiaryEntry().getProject(),null,null,i.getVisibility(),false,"/app/construction-diary/"+i.getDiaryEntry().getId(),"#7c3aed")));
  return result;
 }
 private boolean accept(Project project,CalendarFilters filters){return (filters.projectId()==null||project.getId().equals(filters.projectId()))&&(filters.clientId()==null||project.getClient().getId().equals(filters.clientId()));}
}
