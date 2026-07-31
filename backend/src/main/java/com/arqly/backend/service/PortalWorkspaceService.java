package com.arqly.backend.service;

import com.arqly.backend.dto.PortalDtos.PortalActivityResponse;
import com.arqly.backend.dto.PortalDtos.PortalApprovalResponse;
import com.arqly.backend.dto.PortalDtos.PortalClientResponse;
import com.arqly.backend.dto.PortalDtos.PortalDashboardResponse;
import com.arqly.backend.dto.PortalDtos.PortalDocumentResponse;
import com.arqly.backend.dto.PortalDtos.PortalFileResponse;
import com.arqly.backend.dto.PortalDtos.PortalPhaseResponse;
import com.arqly.backend.dto.PortalDtos.PortalProjectResponse;
import com.arqly.backend.dto.PortalDtos.PortalProjectSummaryResponse;
import com.arqly.backend.dto.PortalDtos.PortalProposalSummaryResponse;
import com.arqly.backend.dto.PortalDtos.PortalStageResponse;
import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityVisibility;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResource;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.FileVisibility;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ActivityRepository;
import com.arqly.backend.repository.ApprovalRepository;
import com.arqly.backend.repository.FileResourceRepository;
import com.arqly.backend.repository.GeneratedDocumentRepository;
import com.arqly.backend.repository.ProjectPhaseRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.service.file.StorageProvider;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalWorkspaceService {
    private static final Set<String> PREVIEW_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp", "svg", "txt", "md", "markdown");

    private final PortalAuthorizationService authorizationService;
    private final ProjectRepository projectRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectStageRepository stageRepository;
    private final ProposalRepository proposalRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final FileResourceRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalService approvalService;
    private final SettingsService settingsService;
    private final StorageProvider storageProvider;

    public PortalWorkspaceService(PortalAuthorizationService authorizationService, ProjectRepository projectRepository,
                                  ProjectPhaseRepository phaseRepository, ProjectStageRepository stageRepository,
                                  ProposalRepository proposalRepository, GeneratedDocumentRepository documentRepository,
                                  FileResourceRepository fileRepository, ActivityRepository activityRepository,
                                  ApprovalRepository approvalRepository, ApprovalService approvalService,
                                  SettingsService settingsService, StorageProvider storageProvider) {
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
        this.phaseRepository = phaseRepository;
        this.stageRepository = stageRepository;
        this.proposalRepository = proposalRepository;
        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.activityRepository = activityRepository;
        this.approvalRepository = approvalRepository;
        this.approvalService = approvalService;
        this.settingsService = settingsService;
        this.storageProvider = storageProvider;
    }

    @Transactional
    public PortalDashboardResponse dashboard(UUID token) {
        var client = authorizationService.client(token);
        var tenantId = client.getTenant().getId();
        var projects = projects(client);
        var proposals = proposals(client, token);
        var activities = activityRepository.findTop20ByTenantIdAndClientIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(
                tenantId, client.getId(), ActivityVisibility.CLIENT_VISIBLE).stream().map(this::activity).toList();
        var lastUpdate = activities.stream().map(PortalActivityResponse::createdAt).findFirst()
                .orElse(projects.stream().map(PortalProjectSummaryResponse::updatedAt).findFirst().orElse(null));
        return new PortalDashboardResponse(client(client), projects.size(),
                projects.stream().filter(project -> project.status() == ProjectStatus.IN_PROGRESS || project.status() == ProjectStatus.PLANNING).count(),
                projects.stream().filter(project -> project.status() == ProjectStatus.COMPLETED).count(),
                proposals.stream().filter(proposal -> proposal.status() == ProposalStatus.SENT || proposal.status() == ProposalStatus.VIEWED).count(),
                documentRepository.countByClientIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalse(client.getId(), tenantId),
                approvalRepository.countByClientIdAndTenantIdAndStatusAndDeletedFalse(client.getId(), tenantId, com.arqly.backend.entity.ApprovalStatus.PENDING),
                lastUpdate, activities);
    }

    @Transactional
    public List<PortalProjectSummaryResponse> projects(UUID token) {
        return projects(authorizationService.client(token));
    }

    @Transactional
    public PortalProjectResponse project(UUID token, UUID projectId) {
        var client = authorizationService.client(token);
        var project = authorizationService.project(client, projectId);
        var tenantId = client.getTenant().getId();
        var stages = stageRepository.findAllByProjectIdOrdered(project.getId(), tenantId);
        var phases = phaseRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(project.getId(), tenantId)
                .stream()
                .map(phase -> new PortalPhaseResponse(phase.getId(), phase.getName(), phase.getColor(), phase.getIcon(),
                        phase.getStatus(), phase.getCompletionPercentage(), stages.stream()
                        .filter(stage -> stage.getProjectPhase().getId().equals(phase.getId()))
                        .map(stage -> new PortalStageResponse(stage.getId(), stage.getName(), stage.getStatus(),
                                stage.getCompletionPercentage(), responsibleName(stage), stage.getPlannedEnd()))
                        .toList()))
                .toList();
        return new PortalProjectResponse(project.getId(), project.getCode(), project.getName(), project.getDescription(),
                project.getStatus(), progress(project), responsibleName(project), displayName(client),
                project.getProposal() == null ? null : project.getProposal().getNumber(), project.getStartDate(),
                project.getExpectedEndDate(), phases, documentsForProject(project), filesForProject(project),
                approvalRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(project.getId(), tenantId)
                        .stream().map(approval -> toPortalApproval(approvalService.toResponse(approval))).toList(),
                activityRepository.findTop20ByTenantIdAndProjectIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(
                        tenantId, project.getId(), ActivityVisibility.CLIENT_VISIBLE).stream().map(this::activity).toList());
    }

    @Transactional
    public List<PortalProposalSummaryResponse> proposals(UUID token) {
        return proposals(authorizationService.client(token), token);
    }

    @Transactional
    public List<PortalDocumentResponse> documents(UUID token) {
        var client = authorizationService.client(token);
        return documentRepository.findAllByClientIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalseOrderByGeneratedAtDesc(
                client.getId(), client.getTenant().getId()).stream().map(this::document).toList();
    }

    @Transactional
    public List<PortalFileResponse> files(UUID token) {
        var client = authorizationService.client(token);
        return projectsRaw(client).stream().flatMap(project -> filesForProject(project).stream()).toList();
    }

    @Transactional
    public List<PortalApprovalResponse> approvals(UUID token) {
        var client = authorizationService.client(token);
        return approvalRepository.findAllByClientIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(client.getId(), client.getTenant().getId())
                .stream().map(approval -> toPortalApproval(approvalService.toResponse(approval))).toList();
    }

    @Transactional
    public PortalApprovalResponse approve(UUID token, UUID approvalId, com.arqly.backend.dto.ApprovalDtos.ApprovalDecisionRequest request) {
        var client = authorizationService.client(token);
        return toPortalApproval(approvalService.decide(client.getTenant().getId(), client.getId(), approvalId, true, request));
    }

    @Transactional
    public PortalApprovalResponse reject(UUID token, UUID approvalId, com.arqly.backend.dto.ApprovalDtos.ApprovalDecisionRequest request) {
        var client = authorizationService.client(token);
        return toPortalApproval(approvalService.decide(client.getTenant().getId(), client.getId(), approvalId, false, request));
    }

    @Transactional(readOnly = true)
    public PortalDocumentResponse document(UUID token, UUID id) {
        return document(documentEntity(token, id));
    }

    @Transactional(readOnly = true)
    public com.arqly.backend.entity.GeneratedDocument documentEntity(UUID token, UUID id) {
        var client = authorizationService.client(token);
        return documentRepository.findByIdAndTenantIdAndClientIdAndClientVisibleTrueAndDeletedFalse(
                id, client.getTenant().getId(), client.getId()).orElseThrow(() -> new NotFoundException("Documento não encontrado no portal."));
    }

    @Transactional(readOnly = true)
    public FileResource portalFile(UUID token, UUID id) {
        var client = authorizationService.client(token);
        var file = fileRepository.findByIdAndTenantId(id, client.getTenant().getId())
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado no portal."));
        if (file.getVisibility() != FileVisibility.CLIENT_VISIBLE || file.getStatus() != FileResourceStatus.ACTIVE || !belongsToClientProject(client, file)) {
            throw new NotFoundException("Arquivo não encontrado no portal.");
        }
        return file;
    }

    @Transactional(readOnly = true)
    public InputStream fileStream(UUID token, UUID id) {
        return storageProvider.load(portalFile(token, id).getStorageKey());
    }

    private List<PortalProjectSummaryResponse> projects(Client client) {
        return projectsRaw(client).stream().map(project -> new PortalProjectSummaryResponse(project.getId(), project.getCode(),
                project.getName(), project.getStatus(), progress(project), responsibleName(project), project.getUpdatedAt())).toList();
    }

    private List<Project> projectsRaw(Client client) {
        return projectRepository.findAllByClientIdAndTenantIdAndDeletedFalseOrderByUpdatedAtDesc(client.getId(), client.getTenant().getId());
    }

    private List<PortalProposalSummaryResponse> proposals(Client client, UUID token) {
        return proposalRepository.findAllByClientIdAndDeletedFalseAndStatusInOrderByCreatedAtDesc(client.getId(),
                List.of(ProposalStatus.SENT, ProposalStatus.VIEWED, ProposalStatus.ACCEPTED)).stream()
                .map(proposal -> new PortalProposalSummaryResponse(proposal.getId(), proposal.getNumber(), proposal.getTitle(),
                        proposal.getStatus(), proposal.getTotal(), proposal.getValidUntil(),
                        settingsService.getGeneral().publicUrl() + "/portal/" + token + "/proposals/" + proposal.getId(),
                        proposal.getCreatedAt()))
                .toList();
    }

    private List<PortalDocumentResponse> documentsForProject(Project project) {
        return documentRepository.findAllByProjectIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalseOrderByGeneratedAtDesc(
                project.getId(), project.getTenant().getId()).stream().map(this::document).toList();
    }

    private List<PortalFileResponse> filesForProject(Project project) {
        var result = new ArrayList<FileResource>();
        result.addAll(fileRepository.findAllByTenantIdAndOwnerTypeAndOwnerIdAndVisibilityAndStatusOrderByUpdatedAtDesc(
                project.getTenant().getId(), FileOwnerType.PROJECT, project.getId(), FileVisibility.CLIENT_VISIBLE, FileResourceStatus.ACTIVE));
        stageRepository.findAllByProjectIdOrdered(project.getId(), project.getTenant().getId()).forEach(stage ->
                result.addAll(fileRepository.findAllByTenantIdAndOwnerTypeAndOwnerIdAndVisibilityAndStatusOrderByUpdatedAtDesc(
                        project.getTenant().getId(), FileOwnerType.PROJECT_STAGE, stage.getId(), FileVisibility.CLIENT_VISIBLE, FileResourceStatus.ACTIVE)));
        return result.stream().sorted(Comparator.comparing(FileResource::getUpdatedAt).reversed()).map(this::file).toList();
    }

    private boolean belongsToClientProject(Client client, FileResource file) {
        if (file.getOwnerType() == FileOwnerType.PROJECT) {
            return projectRepository.findByIdAndTenantIdAndClientIdAndDeletedFalse(file.getOwnerId(), client.getTenant().getId(), client.getId()).isPresent();
        }
        if (file.getOwnerType() == FileOwnerType.PROJECT_STAGE) {
            return stageRepository.findByIdAndTenantIdAndDeletedFalse(file.getOwnerId(), client.getTenant().getId())
                    .filter(stage -> stage.getProjectPhase().getProject().getClient().getId().equals(client.getId()))
                    .isPresent();
        }
        return false;
    }

    private PortalDocumentResponse document(com.arqly.backend.entity.GeneratedDocument document) {
        return new PortalDocumentResponse(document.getId(), document.getTitle(), document.getTemplate().getCategory(),
                document.getVersion(), document.getStatus(), document.getGeneratedAt());
    }

    private PortalFileResponse file(FileResource file) {
        return new PortalFileResponse(file.getId(), file.getOwnerType(), file.getOwnerId(), file.getName(), file.getExtension(),
                file.getMimeType(), file.getSize(), file.getVersion(), file.getVisibility(),
                PREVIEW_EXTENSIONS.contains(file.getExtension().toLowerCase()), file.getUpdatedAt());
    }

    private PortalActivityResponse activity(Activity activity) {
        var content = activity.getContent();
        return new PortalActivityResponse(activity.getId(), activity.getProject() == null ? null : activity.getProject().getId(),
                activity.getStage() == null ? null : activity.getStage().getId(), activity.getAuthorName(),
                activity.getAuthor() == null ? "Sistema" : "Equipe", activity.getType(), content.getTitle(),
                content.getDescription(), activity.getCreatedAt());
    }

    private PortalApprovalResponse toPortalApproval(com.arqly.backend.dto.ApprovalDtos.ApprovalResponse approval) {
        return new PortalApprovalResponse(approval.id(), approval.projectId(), approval.projectName(), approval.stageId(),
                approval.stageName(), approval.documentId(), approval.documentTitle(), approval.status(), approval.description(),
                approval.deadline(), approval.approvedAt(), approval.clientComment(), approval.createdAt());
    }

    private PortalClientResponse client(Client client) {
        return new PortalClientResponse(client.getId(), displayName(client), client.getEmail(), client.getPhone(), client.getCity(), client.getState());
    }

    private java.math.BigDecimal progress(Project project) {
        var phases = phaseRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(project.getId(), project.getTenant().getId());
        if (phases.isEmpty()) return java.math.BigDecimal.ZERO;
        return phases.stream().map(com.arqly.backend.entity.ProjectPhase::getCompletionPercentage)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .divide(java.math.BigDecimal.valueOf(phases.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }

    private String responsibleName(Project project) {
        if (project.getResponsibleUser() != null) return project.getResponsibleUser().getName();
        if (project.getLegacyResponsibleName() != null && !project.getLegacyResponsibleName().isBlank()) return project.getLegacyResponsibleName();
        return "Equipe Arqly";
    }

    private String responsibleName(com.arqly.backend.entity.ProjectStage stage) {
        if (stage.getResponsibleUser() != null) return stage.getResponsibleUser().getName();
        if (stage.getProjectPhase().getProject().getResponsibleUser() != null) return stage.getProjectPhase().getProject().getResponsibleUser().getName();
        return "Equipe Arqly";
    }
}
