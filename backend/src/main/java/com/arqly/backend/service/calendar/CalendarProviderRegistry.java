package com.arqly.backend.service.calendar;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalendarProviderRegistry {
    private final List<CalendarEventProvider> providers;
    public CalendarProviderRegistry(List<CalendarEventProvider> providers) { this.providers = providers; }
    public List<CalendarEventProvider> providers() { return providers; }
}
