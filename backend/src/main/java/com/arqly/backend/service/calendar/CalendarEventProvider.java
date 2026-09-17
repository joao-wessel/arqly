package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import java.time.LocalDate;
import java.util.*;

public interface CalendarEventProvider {
    List<CalendarItemResponse> provide(UUID tenantId, LocalDate start, LocalDate end, CalendarFilters filters);
}
