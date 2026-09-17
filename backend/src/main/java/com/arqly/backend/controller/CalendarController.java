package com.arqly.backend.controller;

import com.arqly.backend.dto.*;
import com.arqly.backend.dto.CalendarDtos.*;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.calendar.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/calendar")
public class CalendarController {
 private final CalendarQueryService query; private final CalendarEventService events;
 public CalendarController(CalendarQueryService query,CalendarEventService events){this.query=query;this.events=events;}
 @GetMapping public ApiResponse<List<CalendarItemResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,@RequestParam LocalDate start,@RequestParam LocalDate end,@RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID clientId,@RequestParam(required=false) UUID responsibleUserId,@RequestParam(required=false) String sourceType,@RequestParam(required=false) String status,@RequestParam(defaultValue="false") boolean mine,@RequestParam(required=false) String search){return ApiResponse.ok(query.query(user.getTenantId(),start,end,new CalendarFilters(projectId,clientId,mine?user.getId():responsibleUserId,sourceType,status,mine,search)));}
 @GetMapping("/events/{id}") public ApiResponse<CalendarEventResponse> get(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(events.get(u.getTenantId(),id));}
 @PostMapping("/events") public ApiResponse<CalendarEventResponse> create(@AuthenticationPrincipal AuthenticatedUser u,@Valid @RequestBody CalendarEventRequest request){return ApiResponse.ok(events.create(u.getTenantId(),u.getId(),request));}
 @PutMapping("/events/{id}") public ApiResponse<CalendarEventResponse> update(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id,@Valid @RequestBody CalendarEventRequest request){return ApiResponse.ok(events.update(u.getTenantId(),u.getId(),id,request));}
 @PostMapping("/events/{id}/complete") public ApiResponse<CalendarEventResponse> complete(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(events.complete(u.getTenantId(),u.getId(),id));}
 @PostMapping("/events/{id}/cancel") public ApiResponse<CalendarEventResponse> cancel(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(events.cancel(u.getTenantId(),u.getId(),id));}
 @DeleteMapping("/events/{id}") public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){events.delete(u.getTenantId(),u.getId(),id);return ApiResponse.message("Compromisso removido.");}
}
