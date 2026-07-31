package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityBuildRequest;
import com.arqly.backend.activity.ActivityFactory;
import com.arqly.backend.activity.ActivityRequestedEvent;
import com.arqly.backend.dto.ActivityDtos.ActivityResponse;
import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ActivityVisibility;
import com.arqly.backend.entity.Role;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ActivityRepository;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.GeneratedDocumentRepository;
import com.arqly.backend.repository.ProjectPhaseRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final ProposalRepository proposalRepository;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectStageRepository stageRepository;
    private final TenantUserRepository userRepository;
    private final EnumMap<ActivityType, ActivityFactory> factories = new EnumMap<>(ActivityType.class);
    private final ActivityFactory systemFactory;

    public ActivityService(ActivityRepository activityRepository,
                           TenantRepository tenantRepository,
                           ProjectRepository projectRepository,
                           ClientRepository clientRepository,
                           ProposalRepository proposalRepository,
                           GeneratedDocumentRepository generatedDocumentRepository,
                           ProjectPhaseRepository phaseRepository,
                           ProjectStageRepository stageRepository,
                           TenantUserRepository userRepository,
                           List<ActivityFactory> activityFactories) {
        this.activityRepository = activityRepository;
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.proposalRepository = proposalRepository;
        this.generatedDocumentRepository = generatedDocumentRepository;
        this.phaseRepository = phaseRepository;
        this.stageRepository = stageRepository;
        this.userRepository = userRepository;
        activityFactories.forEach(factory -> factories.put(factory.type(), factory));
        this.systemFactory = factories.get(ActivityType.SYSTEM);
    }

    @Transactional
    public ActivityResponse create(ActivityRequestedEvent event) {
        var tenant = tenantRepository.findById(event.tenantId()).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var project = event.projectId() == null ? null : projectRepository.findByIdAndTenantIdAndDeletedFalse(event.projectId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
        var client = event.clientId() == null ? null : clientRepository.findByIdAndTenantIdAndDeletedFalse(event.clientId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        var proposal = event.proposalId() == null ? null : proposalRepository.findByIdAndTenantIdAndDeletedFalse(event.proposalId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
        var generatedDocument = event.generatedDocumentId() == null ? null
                : generatedDocumentRepository.findByIdAndTenantIdAndDeletedFalse(event.generatedDocumentId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
        var phase = event.phaseId() == null ? null : phaseRepository.findByIdAndTenantIdAndDeletedFalse(event.phaseId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Fase não encontrada."));
        var stage = event.stageId() == null ? null : stageRepository.findByIdAndTenantIdAndDeletedFalse(event.stageId(), event.tenantId())
                .orElseThrow(() -> new NotFoundException("Etapa não encontrada."));
        if (phase != null && (project == null || !phase.getProject().getId().equals(project.getId()))) {
            throw new BusinessException("A fase informada não pertence ao projeto.");
        }
        if (stage != null && (project == null || !stage.getProjectPhase().getProject().getId().equals(project.getId()))) {
            throw new BusinessException("A etapa informada não pertence ao projeto.");
        }
        if (stage != null && phase != null && !stage.getProjectPhase().getId().equals(phase.getId())) {
            throw new BusinessException("A etapa informada não pertence à fase.");
        }
        if (project != null && client != null && !project.getClient().getId().equals(client.getId())) {
            throw new BusinessException("O cliente informado não pertence ao projeto.");
        }
        if (proposal != null && client != null && !proposal.getClient().getId().equals(client.getId())) {
            throw new BusinessException("O cliente informado não pertence à proposta.");
        }
        var author = event.authorId() == null ? null : userRepository.findByIdAndTenantId(event.authorId(), event.tenantId()).orElse(null);
        var factory = factories.getOrDefault(event.type(), systemFactory);
        var activity = factory.build(new ActivityBuildRequest(tenant, project, client, proposal, generatedDocument, phase, stage, author,
                event.authorName(), event.type(), event.visibility(), event.title(), event.description(), event.metadata()));
        return toResponse(activityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> list(UUID tenantId, UUID projectId, UUID clientId, UUID proposalId, UUID stageId,
                                       ActivityFilter filter, String search, Pageable pageable) {
        return activityRepository.findAll(specification(tenantId, projectId, clientId, proposalId, stageId, filter, search), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ActivityResponse addComment(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId, UUID authorId, String authorName, String comment) {
        return create(new ActivityRequestedEvent(tenantId, projectId, null, null, null, phaseId, stageId, authorId, authorName,
                ActivityType.COMMENT, ActivityVisibility.INTERNAL, "Comentário", comment, null));
    }

    @Transactional
    public ActivityResponse updateComment(UUID tenantId, UUID activityId, UUID authorId, String comment) {
        var activity = findActivity(tenantId, activityId);
        ensureComment(activity);
        ensureAuthor(activity, authorId);
        activity.getContent().setDescription(comment);
        activity.getContent().setEdited(true);
        activity.getContent().setEditedAt(Instant.now());
        return toResponse(activity);
    }

    @Transactional
    public void deleteComment(UUID tenantId, UUID activityId, UUID authorId) {
        var activity = findActivity(tenantId, activityId);
        ensureComment(activity);
        ensureAuthor(activity, authorId);
        activity.setDeleted(true);
        activity.setDeletedAt(Instant.now());
        activity.getContent().setDescription("Comentário removido.");
    }

    public ActivityResponse toResponse(Activity activity) {
        var content = activity.getContent();
        return new ActivityResponse(activity.getId(), activity.getProject() == null ? null : activity.getProject().getId(),
                activity.getClient() == null ? null : activity.getClient().getId(),
                activity.getProposal() == null ? null : activity.getProposal().getId(),
                activity.getGeneratedDocument() == null ? null : activity.getGeneratedDocument().getId(),
                activity.getPhase() == null ? null : activity.getPhase().getId(),
                activity.getStage() == null ? null : activity.getStage().getId(),
                activity.getAuthor() == null ? null : activity.getAuthor().getId(),
                activity.getAuthorName(), authorRole(activity), activity.getType(), activity.getVisibility(),
                content.getTitle(), content.getDescription(), content.getMetadata(),
                content.isEdited(), activity.isDeleted(), content.getEditedAt(), activity.getCreatedAt());
    }

    private Specification<Activity> specification(UUID tenantId, UUID projectId, UUID clientId, UUID proposalId,
                                                    UUID stageId, ActivityFilter filter, String search) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.or(
                    builder.isFalse(root.get("deleted")),
                    builder.and(builder.isTrue(root.get("deleted")), builder.equal(root.get("type"), ActivityType.COMMENT))
            ));
            if (projectId != null) predicates.add(builder.equal(root.get("project").get("id"), projectId));
            if (clientId != null) predicates.add(builder.equal(root.get("client").get("id"), clientId));
            if (proposalId != null) predicates.add(builder.equal(root.get("proposal").get("id"), proposalId));
            if (stageId != null) predicates.add(builder.equal(root.get("stage").get("id"), stageId));
            if (filter != null && !filter.types().isEmpty()) predicates.add(root.get("type").in(filter.types()));
            if (search != null && !search.isBlank()) {
                var term = "%" + search.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.join("content").get("description")), term));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Activity findActivity(UUID tenantId, UUID activityId) {
        return activityRepository.findByIdAndTenantIdAndDeletedFalse(activityId, tenantId)
                .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));
    }

    private String authorRole(Activity activity) {
        if (activity.getAuthor() == null) return "Sistema";
        return activity.getAuthor().getRoles().contains(Role.ROLE_TENANT_ADMIN) ? "Administrador" : "Usuário comum";
    }

    private void ensureComment(Activity activity) {
        if (activity.getType() != ActivityType.COMMENT) throw new BusinessException("Somente comentários podem ser editados ou removidos.");
    }

    private void ensureAuthor(Activity activity, UUID authorId) {
        if (activity.getAuthor() == null || !activity.getAuthor().getId().equals(authorId)) {
            throw new BusinessException("Somente o autor pode editar ou excluir este comentário.");
        }
    }

    public enum ActivityFilter {
        ALL(List.of()),
        COMMENTS(List.of(ActivityType.COMMENT)),
        SYSTEM(List.of(ActivityType.SYSTEM, ActivityType.PROJECT_CREATED, ActivityType.PROJECT_UPDATED, ActivityType.PROJECT_COMPLETED, ActivityType.PHASE_CREATED, ActivityType.PHASE_UPDATED, ActivityType.STAGE_CREATED, ActivityType.STAGE_UPDATED, ActivityType.STAGE_COMPLETED)),
        CHECKLIST(List.of(ActivityType.CHECKLIST_UPDATED)),
        STATUS(List.of(ActivityType.STATUS_CHANGED)),
        FILES(List.of(
                ActivityType.FILE_UPLOADED,
                ActivityType.FILE_VERSIONED,
                ActivityType.FILE_DOWNLOADED,
                ActivityType.FILE_ARCHIVED,
                ActivityType.FILE_RESTORED,
                ActivityType.FILE_MOVED,
                ActivityType.FILE_RENAMED
        )),
        APPROVALS(List.of(ActivityType.CLIENT_APPROVAL));

        private final List<ActivityType> types;
        ActivityFilter(List<ActivityType> types) { this.types = types; }
        public List<ActivityType> types() { return types; }
    }
}
