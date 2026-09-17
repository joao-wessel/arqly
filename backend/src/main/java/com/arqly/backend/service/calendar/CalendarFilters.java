package com.arqly.backend.service.calendar;

import java.util.*;

public record CalendarFilters(UUID projectId, UUID clientId, UUID responsibleUserId, String sourceType,
                              String status, boolean mine, String search) {}
