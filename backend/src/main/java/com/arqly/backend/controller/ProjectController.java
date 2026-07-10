package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ProjectDtos.CreateProjectFromProposalRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectStageRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectStageResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectStatsResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectSummaryResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectTemplateResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectUpdateRequest;
import com.arqly.backend.dto.ProjectDtos.ReorderRequest;
import com.arqly.backend.dto.ProjectDtos.StageTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.StageTemplateResponse;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ProjectManagementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/projects")
public class ProjectController {
    private final ProjectManagementService service;

    public ProjectController(ProjectManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<ProjectSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestParam(required = false) UUID clientId,
                                                          @RequestParam(required = false) String responsible,
                                                          @RequestParam(required = false) ProjectStatus status,
                                                          @RequestParam(required = false) UUID templateId,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to,
                                                          Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), clientId, responsible, status, templateId, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(user.getTenantId(), id));
    }

    @PostMapping("/from-proposal/{proposalId}")
    public ApiResponse<ProjectResponse> createFromProposal(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable UUID proposalId,
                                                           @Valid @RequestBody CreateProjectFromProposalRequest request) {
        return ApiResponse.ok(service.createFromProposal(user.getTenantId(), proposalId, user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), id, user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.getTenantId(), id);
        return ApiResponse.message("Projeto removido.");
    }

    @GetMapping("/stats")
    public ApiResponse<ProjectStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.stats(user.getTenantId()));
    }

    @GetMapping("/templates")
    public ApiResponse<Page<ProjectTemplateResponse>> templates(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return ApiResponse.ok(service.listTemplates(user.getTenantId(), pageable));
    }

    @GetMapping("/templates/options")
    public ApiResponse<List<ProjectTemplateResponse>> templateOptions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.templateOptions(user.getTenantId()));
    }

    @PostMapping("/templates")
    public ApiResponse<ProjectTemplateResponse> createTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @Valid @RequestBody ProjectTemplateRequest request) {
        return ApiResponse.ok(service.createTemplate(user.getTenantId(), request));
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<ProjectTemplateResponse> updateTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @PathVariable UUID id,
                                                               @Valid @RequestBody ProjectTemplateRequest request) {
        return ApiResponse.ok(service.updateTemplate(user.getTenantId(), id, request));
    }

    @PatchMapping("/templates/{id}/delete")
    public ApiResponse<Void> deleteTemplate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.deleteTemplate(user.getTenantId(), id);
        return ApiResponse.message("Modelo removido.");
    }

    @PostMapping("/templates/{id}/duplicate")
    public ApiResponse<ProjectTemplateResponse> duplicateTemplate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.duplicateTemplate(user.getTenantId(), id));
    }

    @GetMapping("/templates/{templateId}/stages")
    public ApiResponse<List<StageTemplateResponse>> templateStages(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @PathVariable UUID templateId) {
        return ApiResponse.ok(service.listStageTemplates(user.getTenantId(), templateId));
    }

    @PostMapping("/templates/{templateId}/stages")
    public ApiResponse<StageTemplateResponse> createTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @Valid @RequestBody StageTemplateRequest request) {
        return ApiResponse.ok(service.createStageTemplate(user.getTenantId(), templateId, request));
    }

    @PutMapping("/templates/{templateId}/stages/{stageId}")
    public ApiResponse<StageTemplateResponse> updateTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @PathVariable UUID stageId,
                                                                  @Valid @RequestBody StageTemplateRequest request) {
        return ApiResponse.ok(service.updateStageTemplate(user.getTenantId(), templateId, stageId, request));
    }

    @PostMapping("/templates/{templateId}/stages/{stageId}/duplicate")
    public ApiResponse<StageTemplateResponse> duplicateTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable UUID templateId,
                                                                    @PathVariable UUID stageId) {
        return ApiResponse.ok(service.duplicateStageTemplate(user.getTenantId(), templateId, stageId));
    }

    @PatchMapping("/templates/{templateId}/stages/reorder")
    public ApiResponse<List<StageTemplateResponse>> reorderTemplateStages(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @PathVariable UUID templateId,
                                                                         @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderStageTemplates(user.getTenantId(), templateId, request));
    }

    @PatchMapping("/templates/{templateId}/stages/{stageId}/delete")
    public ApiResponse<Void> deleteTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable UUID templateId,
                                                 @PathVariable UUID stageId) {
        service.deleteStageTemplate(user.getTenantId(), templateId, stageId);
        return ApiResponse.message("Etapa removida.");
    }

    @GetMapping("/{projectId}/stages")
    public ApiResponse<List<ProjectStageResponse>> stages(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID projectId) {
        return ApiResponse.ok(service.listStages(user.getTenantId(), projectId));
    }

    @PostMapping("/{projectId}/stages")
    public ApiResponse<ProjectStageResponse> createStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID projectId,
                                                        @Valid @RequestBody ProjectStageRequest request) {
        return ApiResponse.ok(service.createStage(user.getTenantId(), projectId, user.getUsername(), request));
    }

    @PutMapping("/{projectId}/stages/{stageId}")
    public ApiResponse<ProjectStageResponse> updateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID projectId,
                                                        @PathVariable UUID stageId,
                                                        @Valid @RequestBody ProjectStageRequest request) {
        return ApiResponse.ok(service.updateStage(user.getTenantId(), projectId, stageId, user.getUsername(), request));
    }

    @PostMapping("/{projectId}/stages/{stageId}/duplicate")
    public ApiResponse<ProjectStageResponse> duplicateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable UUID projectId,
                                                           @PathVariable UUID stageId) {
        return ApiResponse.ok(service.duplicateStage(user.getTenantId(), projectId, stageId, user.getUsername()));
    }

    @PatchMapping("/{projectId}/stages/reorder")
    public ApiResponse<List<ProjectStageResponse>> reorderStages(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @PathVariable UUID projectId,
                                                                @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderStages(user.getTenantId(), projectId, request));
    }

    @PatchMapping("/{projectId}/stages/{stageId}/delete")
    public ApiResponse<Void> deleteStage(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID projectId,
                                         @PathVariable UUID stageId) {
        service.deleteStage(user.getTenantId(), projectId, stageId);
        return ApiResponse.message("Etapa removida.");
    }
}
