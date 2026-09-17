package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ConstructionDiaryDtos.*;
import com.arqly.backend.entity.*;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tenant/construction-diary")
public class ConstructionDiaryController {
 private final ConstructionDiaryService service;private final ConstructionDiaryQueryService query;
 public ConstructionDiaryController(ConstructionDiaryService service,ConstructionDiaryQueryService query){this.service=service;this.query=query;}
 @GetMapping public ApiResponse<Page<DiarySummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser u,@RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID stageId,@RequestParam(required=false) UUID responsibleUserId,@RequestParam(required=false) ConstructionDiaryEntryType type,@RequestParam(required=false) ConstructionDiaryStatus status,@RequestParam(required=false) ActivityVisibility visibility,@RequestParam(required=false) Boolean critical,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) String text,Pageable pageable){return ApiResponse.ok(query.list(u.getTenantId(),projectId,stageId,responsibleUserId,type,status,visibility,critical,from,to,text,pageable));}
 @GetMapping("/stats") public ApiResponse<DiaryStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser u){return ApiResponse.ok(query.stats(u.getTenantId()));}
 @GetMapping("/{id}") public ApiResponse<DiaryResponse> get(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(query.get(u.getTenantId(),id));}
 @GetMapping("/project/{projectId}") public ApiResponse<List<DiarySummaryResponse>> project(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID projectId){return ApiResponse.ok(query.project(u.getTenantId(),projectId));}
 @GetMapping("/project/{projectId}/stage/{stageId}") public ApiResponse<List<DiarySummaryResponse>> stage(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID projectId,@PathVariable UUID stageId){return ApiResponse.ok(query.stage(u.getTenantId(),projectId,stageId));}
 @PostMapping public ApiResponse<DiaryResponse> create(@AuthenticationPrincipal AuthenticatedUser u,@Valid @RequestBody EntryRequest r){return ApiResponse.ok(service.create(u.getTenantId(),u.getId(),u.getUsername(),r));}
 @PutMapping("/{id}") public ApiResponse<DiaryResponse> update(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id,@Valid @RequestBody EntryRequest r){return ApiResponse.ok(service.update(u.getTenantId(),id,u.getId(),u.getUsername(),r));}
 @PostMapping("/{id}/publish") public ApiResponse<DiaryResponse> publish(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(service.publish(u.getTenantId(),id,u.getId(),u.getUsername()));}
 @PatchMapping("/{id}/archive") public ApiResponse<DiaryResponse> archive(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){return ApiResponse.ok(service.archive(u.getTenantId(),id,u.getId(),u.getUsername()));}
 @DeleteMapping("/{id}") public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id){service.delete(u.getTenantId(),id);return ApiResponse.message("Rascunho removido.");}
 @PostMapping("/{id}/photos") public ApiResponse<DiaryResponse> photo(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id,@Valid @RequestBody PhotoRequest r){return ApiResponse.ok(service.addPhoto(u.getTenantId(),id,u.getId(),u.getUsername(),r));}
 @PostMapping("/{id}/occurrences") public ApiResponse<DiaryResponse> occurrence(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id,@Valid @RequestBody OccurrenceRequest r){return ApiResponse.ok(service.addOccurrence(u.getTenantId(),id,u.getId(),u.getUsername(),r));}
 @PostMapping("/{id}/decisions") public ApiResponse<DiaryResponse> decision(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable UUID id,@Valid @RequestBody DecisionRequest r){return ApiResponse.ok(service.addDecision(u.getTenantId(),id,u.getId(),u.getUsername(),r));}
}
