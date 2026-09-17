package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarQueryService {
 private final CalendarProviderRegistry registry;
 public CalendarQueryService(CalendarProviderRegistry registry){this.registry=registry;}
 @Transactional(readOnly=true)
 public List<CalendarItemResponse> query(UUID tenantId,LocalDate start,LocalDate end,CalendarFilters filters){
  if(end.isBefore(start)||start.plusMonths(18).isBefore(end))throw new IllegalArgumentException("Informe um intervalo de até 18 meses.");
  return registry.providers().stream().flatMap(p->p.provide(tenantId,start,end,filters).stream()).sorted(Comparator.comparing(CalendarItemResponse::start)).toList();
 }
}
