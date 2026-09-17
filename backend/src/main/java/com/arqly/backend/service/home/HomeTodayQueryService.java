package com.arqly.backend.service.home;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.service.calendar.CalendarFilters;
import com.arqly.backend.service.calendar.CalendarQueryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeTodayQueryService {
    private final CalendarQueryService calendar;
    public HomeTodayQueryService(CalendarQueryService calendar) { this.calendar = calendar; }
    @Transactional(readOnly = true)
    public List<CalendarItemResponse> today(HomeUserContext context) {
        LocalDate today = LocalDate.now();
        return calendar.query(context.tenantId(), today, today,
                new CalendarFilters(null, null, context.userId(), null, null, true, null)).stream().limit(12).toList();
    }
}
