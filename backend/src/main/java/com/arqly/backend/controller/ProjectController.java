package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ProjectDtos.CreateProjectFromProposalRequest;
import com.arqly.backend.dto.ProjectDtos.CreateProjectManualRequest;
import com.arqly.backend.dto.ProjectDtos.PhaseTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.PhaseTemplateResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectPhaseRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectPhaseResponse;
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

    @PostMapping("/manual")
    public ApiResponse<ProjectResponse> createManual(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody CreateProjectManualRequest request) {
        return ApiResponse.ok(service.createManual(user.getTenantId(), user.getUsername(), request));
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

    @GetMapping("/templates/{templateId}/phases")
    public ApiResponse<List<PhaseTemplateResponse>> templatePhases(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @PathVariable UUID templateId) {
        return ApiResponse.ok(service.listPhaseTemplates(user.getTenantId(), templateId));
    }

    @PostMapping("/templates/{templateId}/phases")
    public ApiResponse<PhaseTemplateResponse> createTemplatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @Valid @RequestBody PhaseTemplateRequest request) {
        return ApiResponse.ok(service.createPhaseTemplate(user.getTenantId(), templateId, request));
    }

    @PutMapping("/templates/{templateId}/phases/{phaseId}")
    public ApiResponse<PhaseTemplateResponse> updateTemplatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @PathVariable UUID phaseId,
                                                                  @Valid @RequestBody PhaseTemplateRequest request) {
        return ApiResponse.ok(service.updatePhaseTemplate(user.getTenantId(), templateId, phaseId, request));
    }

    @PostMapping("/templates/{templateId}/phases/{phaseId}/duplicate")
    public ApiResponse<PhaseTemplateResponse> duplicateTemplatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable UUID templateId,
                                                                    @PathVariable UUID phaseId) {
        return ApiResponse.ok(service.duplicatePhaseTemplate(user.getTenantId(), templateId, phaseId));
    }

    @PatchMapping("/templates/{templateId}/phases/reorder")
    public ApiResponse<List<PhaseTemplateResponse>> reorderTemplatePhases(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @PathVariable UUID templateId,
                                                                         @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderPhaseTemplates(user.getTenantId(), templateId, request));
    }

    @PatchMapping("/templates/{templateId}/phases/{phaseId}/delete")
    public ApiResponse<Void> deleteTemplatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable UUID templateId,
                                                 @PathVariable UUID phaseId) {
        service.deletePhaseTemplate(user.getTenantId(), templateId, phaseId);
        return ApiResponse.message("Fase removida.");
    }

    @PostMapping("/templates/{templateId}/phases/{phaseId}/stages")
    public ApiResponse<StageTemplateResponse> createTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @PathVariable UUID phaseId,
                                                                  @Valid @RequestBody StageTemplateRequest request) {
        return ApiResponse.ok(service.createStageTemplate(user.getTenantId(), templateId, phaseId, request));
    }

    @PutMapping("/templates/{templateId}/phases/{phaseId}/stages/{stageId}")
    public ApiResponse<StageTemplateResponse> updateTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID templateId,
                                                                  @PathVariable UUID phaseId,
                                                                  @PathVariable UUID stageId,
                                                                  @Valid @RequestBody StageTemplateRequest request) {
        return ApiResponse.ok(service.updateStageTemplate(user.getTenantId(), templateId, phaseId, stageId, request));
    }

    @PostMapping("/templates/{templateId}/phases/{phaseId}/stages/{stageId}/duplicate")
    public ApiResponse<StageTemplateResponse> duplicateTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable UUID templateId,
                                                                    @PathVariable UUID phaseId,
                                                                    @PathVariable UUID stageId) {
        return ApiResponse.ok(service.duplicateStageTemplate(user.getTenantId(), templateId, phaseId, stageId));
    }

    @PatchMapping("/templates/{templateId}/phases/{phaseId}/stages/reorder")
    public ApiResponse<List<PhaseTemplateResponse>> reorderTemplateStages(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @PathVariable UUID templateId,
                                                                         @PathVariable UUID phaseId,
                                                                         @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderStageTemplates(user.getTenantId(), templateId, phaseId, request));
    }

    @PatchMapping("/templates/{templateId}/phases/{phaseId}/stages/{stageId}/delete")
    public ApiResponse<Void> deleteTemplateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable UUID templateId,
                                                 @PathVariable UUID phaseId,
                                                 @PathVariable UUID stageId) {
        service.deleteStageTemplate(user.getTenantId(), templateId, phaseId, stageId);
        return ApiResponse.message("Etapa removida.");
    }

    @GetMapping("/{projectId}/phases")
    public ApiResponse<List<ProjectPhaseResponse>> phases(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID projectId) {
        return ApiResponse.ok(service.listPhases(user.getTenantId(), projectId));
    }

    @PostMapping("/{projectId}/phases")
    public ApiResponse<ProjectPhaseResponse> createPhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @PathVariable UUID projectId,
                                                         @Valid @RequestBody ProjectPhaseRequest request) {
        return ApiResponse.ok(service.createPhase(user.getTenantId(), projectId, user.getUsername(), request));
    }

    @PutMapping("/{projectId}/phases/{phaseId}")
    public ApiResponse<ProjectPhaseResponse> updatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @PathVariable UUID projectId,
                                                         @PathVariable UUID phaseId,
                                                         @Valid @RequestBody ProjectPhaseRequest request) {
        return ApiResponse.ok(service.updatePhase(user.getTenantId(), projectId, phaseId, user.getUsername(), request));
    }

    @PostMapping("/{projectId}/phases/{phaseId}/duplicate")
    public ApiResponse<ProjectPhaseResponse> duplicatePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable UUID projectId,
                                                           @PathVariable UUID phaseId) {
        return ApiResponse.ok(service.duplicatePhase(user.getTenantId(), projectId, phaseId, user.getUsername()));
    }

    @PatchMapping("/{projectId}/phases/reorder")
    public ApiResponse<List<ProjectPhaseResponse>> reorderPhases(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @PathVariable UUID projectId,
                                                                 @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderPhases(user.getTenantId(), projectId, request));
    }

    @PatchMapping("/{projectId}/phases/{phaseId}/delete")
    public ApiResponse<Void> deletePhase(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID projectId,
                                         @PathVariable UUID phaseId) {
        service.deletePhase(user.getTenantId(), projectId, phaseId);
        return ApiResponse.message("Fase removida.");
    }

    @PostMapping("/{projectId}/phases/{phaseId}/stages")
    public ApiResponse<ProjectStageResponse> createStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID projectId,
                                                        @PathVariable UUID phaseId,
                                                        @Valid @RequestBody ProjectStageRequest request) {
        return ApiResponse.ok(service.createStage(user.getTenantId(), projectId, phaseId, user.getUsername(), request));
    }

    @PutMapping("/{projectId}/phases/{phaseId}/stages/{stageId}")
    public ApiResponse<ProjectStageResponse> updateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID projectId,
                                                        @PathVariable UUID phaseId,
                                                        @PathVariable UUID stageId,
                                                        @Valid @RequestBody ProjectStageRequest request) {
        return ApiResponse.ok(service.updateStage(user.getTenantId(), projectId, phaseId, stageId, user.getUsername(), request));
    }

    @PostMapping("/{projectId}/phases/{phaseId}/stages/{stageId}/duplicate")
    public ApiResponse<ProjectStageResponse> duplicateStage(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable UUID projectId,
                                                           @PathVariable UUID phaseId,
                                                           @PathVariable UUID stageId) {
        return ApiResponse.ok(service.duplicateStage(user.getTenantId(), projectId, phaseId, stageId, user.getUsername()));
    }

    @PatchMapping("/{projectId}/phases/{phaseId}/stages/reorder")
    public ApiResponse<List<ProjectPhaseResponse>> reorderStages(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @PathVariable UUID projectId,
                                                                @PathVariable UUID phaseId,
                                                                @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(service.reorderStages(user.getTenantId(), projectId, request));
    }

    @PatchMapping("/{projectId}/phases/{phaseId}/stages/{stageId}/delete")
    public ApiResponse<Void> deleteStage(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID projectId,
                                         @PathVariable UUID phaseId,
                                         @PathVariable UUID stageId) {
        service.deleteStage(user.getTenantId(), projectId, phaseId, stageId);
        return ApiResponse.message("Etapa removida.");
    }
}
