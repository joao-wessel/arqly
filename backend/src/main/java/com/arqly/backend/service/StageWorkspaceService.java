package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistItemRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistItemResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistToggleRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.CommentRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.CommentResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.PhaseInfo;
import com.arqly.backend.dto.StageWorkspaceDtos.ProjectInfo;
import com.arqly.backend.dto.StageWorkspaceDtos.ReorderChecklistRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.StageUpdateRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.StageWorkspaceResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.StageWorkspaceStatsResponse;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.ProgressCalculationMode;
import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.ProjectStageChecklistItem;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ProjectPhaseRepository;
import com.arqly.backend.repository.ProjectStageChecklistItemRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageWorkspaceService {
    private final ProjectStageRepository stageRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectStageChecklistItemRepository checklistRepository;
    private final TenantUserRepository tenantUserRepository;
    private final ActivityEventPublisher activityPublisher;
    private final ActivityService activityService;

    public StageWorkspaceService(ProjectStageRepository stageRepository,
                                 ProjectPhaseRepository phaseRepository,
                                 ProjectStageChecklistItemRepository checklistRepository,
                                 TenantUserRepository tenantUserRepository,
                                 ActivityEventPublisher activityPublisher,
                                 ActivityService activityService) {
        this.stageRepository = stageRepository;
        this.phaseRepository = phaseRepository;
        this.checklistRepository = checklistRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.activityPublisher = activityPublisher;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public StageWorkspaceResponse get(UUID tenantId, UUID stageId) {
        return toResponse(tenantId, findStage(tenantId, stageId));
    }

    @Transactional(readOnly = true)
    public StageWorkspaceStatsResponse stats(UUID tenantId) {
        return new StageWorkspaceStatsResponse(
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.IN_PROGRESS),
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.COMPLETED),
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.WAITING_CLIENT),
                checklistRepository.countByTenantIdAndCompletedFalseAndDeletedFalse(tenantId)
        );
    }

    @Transactional
    public StageWorkspaceResponse updateStage(UUID tenantId, UUID stageId, UUID userId, String actor, StageUpdateRequest request) {
        var stage = findStage(tenantId, stageId);
        var oldStatus = stage.getStatus();
        var oldProgress = value(stage.getCompletionPercentage());
        var oldResponsibleId = stage.getResponsibleUser() == null ? null : stage.getResponsibleUser().getId();

        stage.setName(request.name());
        stage.setDescription(request.description());
        stage.setResponsibleUser(request.responsibleUserId() == null ? null : tenantUserRepository.findByIdAndTenantId(request.responsibleUserId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário responsável não encontrado.")));
        stage.setLegacyResponsibleName(request.responsible());
        stage.setPlannedStart(request.plannedStart());
        stage.setPlannedEnd(request.plannedEnd());
        stage.setActualStart(request.actualStart());
        stage.setActualEnd(request.actualEnd());
        stage.setNotes(request.notes());
        stage.setWeightPercentage(scale(request.weightPercentage()));
        if (request.progressCalculationMode() != null) stage.setProgressCalculationMode(request.progressCalculationMode());
        if (request.status() != null) stage.setStatus(request.status());
        if (stage.getProgressCalculationMode() == ProgressCalculationMode.CHECKLIST) {
            applyChecklistProgress(tenantId, stage);
        } else {
            stage.setCompletionPercentage(scale(request.completionPercentage()));
        }

        registerChanges(stage, oldStatus, oldProgress, oldResponsibleId, userId, actor);
        recalculatePhase(tenantId, stage.getProjectPhase().getId());
        return toResponse(tenantId, stage);
    }

    @Transactional
    public StageWorkspaceResponse complete(UUID tenantId, UUID stageId, UUID userId, String actor) {
        var stage = findStage(tenantId, stageId);
        stage.setStatus(ProjectStageStatus.COMPLETED);
        stage.setCompletionPercentage(new BigDecimal("100.00"));
        stage.setActualEnd(LocalDate.now());
        publish(stage, userId, actor, ActivityType.STAGE_COMPLETED, "Etapa concluída", "concluiu a etapa");
        recalculatePhase(tenantId, stage.getProjectPhase().getId());
        return toResponse(tenantId, stage);
    }

    @Transactional
    public StageWorkspaceResponse pause(UUID tenantId, UUID stageId, UUID userId, String actor) {
        var stage = findStage(tenantId, stageId);
        stage.setStatus(ProjectStageStatus.ON_HOLD);
        publish(stage, userId, actor, ActivityType.STATUS_CHANGED, "Status alterado", "alterou o status da etapa para Pausada");
        recalculatePhase(tenantId, stage.getProjectPhase().getId());
        return toResponse(tenantId, stage);
    }

    @Transactional
    public StageWorkspaceResponse cancel(UUID tenantId, UUID stageId, UUID userId, String actor) {
        var stage = findStage(tenantId, stageId);
        stage.setStatus(ProjectStageStatus.CANCELLED);
        publish(stage, userId, actor, ActivityType.STATUS_CHANGED, "Status alterado", "alterou o status da etapa para Cancelada");
        recalculatePhase(tenantId, stage.getProjectPhase().getId());
        return toResponse(tenantId, stage);
    }

    @Transactional
    public ChecklistItemResponse addChecklistItem(UUID tenantId, UUID stageId, UUID userId, String actor, ChecklistItemRequest request) {
        var stage = findStage(tenantId, stageId);
        var item = new ProjectStageChecklistItem();
        item.setTenant(stage.getTenant());
        item.setStage(stage);
        item.setDescription(request.description());
        item.setOrder(request.order());
        item.setNotes(request.notes());
        item = checklistRepository.save(item);
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado",
                "adicionou o item \"" + item.getDescription() + "\"");
        refreshChecklistProgress(tenantId, stage, userId, actor);
        return toChecklistItem(item);
    }

    @Transactional
    public ChecklistItemResponse updateChecklistItem(UUID tenantId, UUID stageId, UUID itemId, UUID userId, String actor, ChecklistItemRequest request) {
        var stage = findStage(tenantId, stageId);
        var item = findChecklistItem(tenantId, itemId);
        ensureItemStage(item, stageId);
        item.setDescription(request.description());
        item.setOrder(request.order());
        item.setNotes(request.notes());
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado",
                "editou o item \"" + item.getDescription() + "\"");
        return toChecklistItem(item);
    }

    @Transactional
    public ChecklistItemResponse duplicateChecklistItem(UUID tenantId, UUID stageId, UUID itemId, UUID userId, String actor) {
        var stage = findStage(tenantId, stageId);
        var source = findChecklistItem(tenantId, itemId);
        ensureItemStage(source, stageId);
        var copy = new ProjectStageChecklistItem();
        copy.setTenant(source.getTenant());
        copy.setStage(stage);
        copy.setDescription(source.getDescription() + " (cópia)");
        copy.setOrder(source.getOrder() + 1);
        copy.setNotes(source.getNotes());
        copy = checklistRepository.save(copy);
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado",
                "duplicou o item \"" + source.getDescription() + "\"");
        refreshChecklistProgress(tenantId, stage, userId, actor);
        return toChecklistItem(copy);
    }

    @Transactional
    public void deleteChecklistItem(UUID tenantId, UUID stageId, UUID itemId, UUID userId, String actor) {
        var stage = findStage(tenantId, stageId);
        var item = findChecklistItem(tenantId, itemId);
        ensureItemStage(item, stageId);
        item.setDeleted(true);
        item.setDeletedAt(Instant.now());
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado",
                "removeu o item \"" + item.getDescription() + "\"");
        refreshChecklistProgress(tenantId, stage, userId, actor);
    }

    @Transactional
    public ChecklistItemResponse toggleChecklistItem(UUID tenantId, UUID stageId, UUID itemId, UUID userId, String actor, ChecklistToggleRequest request) {
        var stage = findStage(tenantId, stageId);
        var item = findChecklistItem(tenantId, itemId);
        ensureItemStage(item, stageId);
        item.setCompleted(request.completed());
        item.setCompletedAt(request.completed() ? Instant.now() : null);
        item.setCompletedBy(request.completed() ? actor : null);
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado",
                (request.completed() ? "concluiu" : "reabriu") + " o item \"" + item.getDescription() + "\"");
        refreshChecklistProgress(tenantId, stage, userId, actor);
        return toChecklistItem(item);
    }

    @Transactional
    public List<ChecklistItemResponse> reorderChecklist(UUID tenantId, UUID stageId, UUID userId, String actor, ReorderChecklistRequest request) {
        var stage = findStage(tenantId, stageId);
        for (var itemRequest : request.items()) {
            var item = findChecklistItem(tenantId, itemRequest.id());
            ensureItemStage(item, stageId);
            item.setOrder(itemRequest.order());
        }
        publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Checklist atualizado", "reordenou o checklist");
        return checklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stageId, tenantId).stream()
                .map(this::toChecklistItem)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(UUID tenantId, UUID stageId, UUID userId, String actor, CommentRequest request) {
        var stage = findStage(tenantId, stageId);
        var activity = activityService.addComment(tenantId, stage.getProjectPhase().getProject().getId(),
                stage.getProjectPhase().getId(), stage.getId(), userId, actor, request.comment());
        return toComment(activity);
    }

    @Transactional
    public CommentResponse updateComment(UUID tenantId, UUID stageId, UUID commentId, UUID userId, String actor, CommentRequest request) {
        findStage(tenantId, stageId);
        return toComment(activityService.updateComment(tenantId, commentId, userId, request.comment()));
    }

    @Transactional
    public void deleteComment(UUID tenantId, UUID stageId, UUID commentId, UUID userId, String actor) {
        findStage(tenantId, stageId);
        activityService.deleteComment(tenantId, commentId, userId);
    }

    private StageWorkspaceResponse toResponse(UUID tenantId, ProjectStage stage) {
        var phase = stage.getProjectPhase();
        var project = phase.getProject();
        var checklist = checklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId).stream()
                .map(this::toChecklistItem)
                .toList();
        var activities = activityService.list(tenantId, project.getId(), null, null, stage.getId(), ActivityService.ActivityFilter.ALL, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        var comments = activities.stream()
                .filter(activity -> activity.type() == ActivityType.COMMENT)
                .map(this::toComment)
                .toList();
        return new StageWorkspaceResponse(stage.getId(), stage.getName(), stage.getDescription(), stage.getStatus(),
                stage.getCompletionPercentage(), stage.getWeightPercentage(), stage.getProgressCalculationMode(),
                stage.getResponsibleUser() == null ? null : stage.getResponsibleUser().getId(), responsibleName(stage), responsibleName(stage),
                stage.getPlannedStart(), stage.getPlannedEnd(), stage.getActualStart(), stage.getActualEnd(),
                remainingDays(stage.getPlannedEnd()), stage.getNotes(),
                new ProjectInfo(project.getId(), project.getCode(), project.getName(), displayName(project.getClient()),
                        project.getProposal() == null ? null : project.getProposal().getId(),
                        project.getProposal() == null ? null : project.getProposal().getNumber()),
                new PhaseInfo(phase.getId(), phase.getName(), phase.getColor(), phase.getIcon(), phase.getCompletionPercentage()),
                checklist, comments, activities);
    }

    private void registerChanges(ProjectStage stage, ProjectStageStatus oldStatus, BigDecimal oldProgress, UUID oldResponsibleId, UUID userId, String actor) {
        if (oldStatus != stage.getStatus()) {
            publish(stage, userId, actor, ActivityType.STATUS_CHANGED, "Status alterado", "alterou o status da etapa para " + statusLabel(stage.getStatus()));
        }
        if (oldProgress.compareTo(value(stage.getCompletionPercentage())) != 0) {
            publish(stage, userId, actor, ActivityType.STAGE_UPDATED, "Percentual alterado", "alterou o percentual da etapa para " + percentLabel(stage.getCompletionPercentage()));
        }
        var responsibleId = stage.getResponsibleUser() == null ? null : stage.getResponsibleUser().getId();
        if (!java.util.Objects.equals(oldResponsibleId, responsibleId)) {
            var assigned = responsibleId != null;
            publish(stage, userId, actor, assigned ? ActivityType.ASSIGNED : ActivityType.UNASSIGNED,
                    "Responsável alterado", assigned ? "atribuiu um responsável à etapa" : "removeu o responsável da etapa");
        }
    }

    private void refreshChecklistProgress(UUID tenantId, ProjectStage stage, UUID userId, String actor) {
        if (stage.getProgressCalculationMode() == ProgressCalculationMode.CHECKLIST) {
            var oldProgress = value(stage.getCompletionPercentage());
            applyChecklistProgress(tenantId, stage);
            if (oldProgress.compareTo(value(stage.getCompletionPercentage())) != 0) {
                publish(stage, userId, actor, ActivityType.CHECKLIST_UPDATED, "Progresso do checklist atualizado",
                        "recalculou o percentual para " + percentLabel(stage.getCompletionPercentage()));
            }
            recalculatePhase(tenantId, stage.getProjectPhase().getId());
        }
    }

    private void applyChecklistProgress(UUID tenantId, ProjectStage stage) {
        var total = checklistRepository.countByStageIdAndTenantIdAndDeletedFalse(stage.getId(), tenantId);
        if (total == 0) {
            stage.setCompletionPercentage(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        var completed = checklistRepository.countByStageIdAndTenantIdAndCompletedTrueAndDeletedFalse(stage.getId(), tenantId);
        stage.setCompletionPercentage(BigDecimal.valueOf(completed)
                .multiply(new BigDecimal("100.00"))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        if (completed == total) {
            stage.setStatus(ProjectStageStatus.COMPLETED);
            if (stage.getActualEnd() == null) stage.setActualEnd(LocalDate.now());
        } else if (stage.getStatus() == ProjectStageStatus.COMPLETED) {
            stage.setStatus(ProjectStageStatus.IN_PROGRESS);
            stage.setActualEnd(null);
        }
    }

    private void recalculatePhase(UUID tenantId, UUID phaseId) {
        var phase = phaseRepository.findByIdAndTenantIdAndDeletedFalse(phaseId, tenantId)
                .orElseThrow(() -> new NotFoundException("Fase não encontrada."));
        var stages = stageRepository.findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phaseId, tenantId);
        if (stages.isEmpty()) {
            phase.setCompletionPercentage(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        var total = stages.stream().map(ProjectStage::getCompletionPercentage).map(this::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        phase.setCompletionPercentage(total.divide(BigDecimal.valueOf(stages.size()), 2, RoundingMode.HALF_UP));
    }

    private void publish(ProjectStage stage, UUID authorId, String actor, ActivityType type, String title, String description) {
        activityPublisher.publish(stage.getTenant().getId(), stage.getProjectPhase().getProject().getId(),
                stage.getProjectPhase().getId(), stage.getId(), authorId, actor, type, title, description);
    }

    private ProjectStage findStage(UUID tenantId, UUID stageId) {
        return stageRepository.findByIdAndTenantIdAndDeletedFalse(stageId, tenantId)
                .orElseThrow(() -> new NotFoundException("Etapa não encontrada."));
    }

    private ProjectStageChecklistItem findChecklistItem(UUID tenantId, UUID itemId) {
        return checklistRepository.findByIdAndTenantIdAndDeletedFalse(itemId, tenantId)
                .orElseThrow(() -> new NotFoundException("Item do checklist não encontrado."));
    }

    private void ensureItemStage(ProjectStageChecklistItem item, UUID stageId) {
        if (!item.getStage().getId().equals(stageId)) {
            throw new NotFoundException("Item do checklist não encontrado nesta etapa.");
        }
    }

    private ChecklistItemResponse toChecklistItem(ProjectStageChecklistItem item) {
        return new ChecklistItemResponse(item.getId(), item.getDescription(), item.isCompleted(), item.getOrder(),
                item.getCompletedAt(), item.getCompletedBy(), item.getNotes());
    }

    private CommentResponse toComment(com.arqly.backend.dto.ActivityDtos.ActivityResponse activity) {
        return new CommentResponse(activity.id(), activity.authorId() == null ? null : activity.authorId().toString(),
                activity.authorName(), activity.description(), activity.edited(), activity.createdAt(),
                activity.editedAt() == null ? activity.createdAt() : activity.editedAt());
    }

    private long remainingDays(LocalDate plannedEnd) {
        return plannedEnd == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), plannedEnd);
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }

    private String responsibleName(ProjectStage stage) {
        if (stage.getResponsibleUser() != null) return stage.getResponsibleUser().getName();
        if (stage.getLegacyResponsibleName() != null && !stage.getLegacyResponsibleName().isBlank()) return stage.getLegacyResponsibleName();
        var projectResponsible = stage.getProjectPhase().getProject().getResponsibleUser();
        if (projectResponsible != null) return projectResponsible.getName();
        return stage.getResponsible();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value(value).max(BigDecimal.ZERO).min(new BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP);
    }

    private String percentLabel(BigDecimal value) {
        return value(value).stripTrailingZeros().toPlainString() + "%";
    }

    private String statusLabel(ProjectStageStatus status) {
        return switch (status) {
            case NOT_STARTED -> "Não iniciada";
            case IN_PROGRESS -> "Em progresso";
            case WAITING_CLIENT -> "Aguardando cliente";
            case WAITING_APPROVAL -> "Aguardando aprovação";
            case ON_HOLD -> "Pausada";
            case COMPLETED -> "Concluída";
            case CANCELLED -> "Cancelada";
        };
    }
}
