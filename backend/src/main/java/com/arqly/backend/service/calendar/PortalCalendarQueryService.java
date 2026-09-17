package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.entity.ActivityVisibility;
import com.arqly.backend.service.PortalAuthorizationService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PortalCalendarQueryService {
 private final PortalAuthorizationService authorization; private final CalendarQueryService query;
 public PortalCalendarQueryService(PortalAuthorizationService authorization,CalendarQueryService query){this.authorization=authorization;this.query=query;}
 public List<CalendarItemResponse> list(UUID token,LocalDate start,LocalDate end){var client=authorization.client(token);return query.query(client.getTenant().getId(),start,end,new CalendarFilters(null,client.getId(),null,null,null,false,null)).stream().filter(i->i.visibility()==ActivityVisibility.CLIENT_VISIBLE).toList();}
}
