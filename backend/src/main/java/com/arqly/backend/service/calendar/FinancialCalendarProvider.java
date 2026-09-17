package com.arqly.backend.service.calendar;
import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import java.time.LocalDate;import java.util.*;import org.springframework.stereotype.Component;
@Component public class FinancialCalendarProvider implements CalendarEventProvider {
 private final FinancialInstallmentRepository installments; private final CalendarItemFactory factory;
 public FinancialCalendarProvider(FinancialInstallmentRepository installments,CalendarItemFactory factory){this.installments=installments;this.factory=factory;}
 public List<CalendarItemResponse> provide(UUID tenant,LocalDate start,LocalDate end,CalendarFilters filters){
  if(filters.sourceType()!=null&&!filters.sourceType().isBlank()&&!"FINANCIAL".equals(filters.sourceType()))return List.of();
  return installments.findAllByEntryTenantIdAndEntryDeletedFalse(tenant).stream().filter(i->i.getStatus()!=FinancialInstallmentStatus.PAID&&i.getStatus()!=FinancialInstallmentStatus.CANCELLED).filter(i->!i.getDueDate().isBefore(start)&&!i.getDueDate().isAfter(end)).filter(i->filters.projectId()==null||(i.getEntry().getProject()!=null&&i.getEntry().getProject().getId().equals(filters.projectId()))).filter(i->filters.clientId()==null||(i.getEntry().getClient()!=null&&i.getEntry().getClient().getId().equals(filters.clientId()))).map(i->{var e=i.getEntry();String verb=e.getType()==FinancialEntryType.RECEIVABLE?"Receber":"Pagar";return factory.allDay(i.getId(),CalendarSourceType.FINANCIAL,verb+" — "+e.getDescription(),i.getDueDate(),e.getType().name(),i.getStatus().name(),e.getProject(),null,e.getProject()==null?null:e.getProject().getResponsibleUser(),e.isClientVisible()?ActivityVisibility.CLIENT_VISIBLE:ActivityVisibility.INTERNAL,false,"/app/financial","#0f766e");}).toList();
 }
}
