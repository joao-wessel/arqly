package com.arqly.backend.service;

import com.arqly.backend.dto.ProjectDtos.CreateProjectFromProposalRequest;
import com.arqly.backend.dto.ProjectDtos.CreateProjectManualRequest;
import com.arqly.backend.dto.ProjectDtos.PhaseTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.PhaseTemplateResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectPhaseRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectPhaseResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectServiceResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectStageRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectStageResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectStatsResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectSummaryResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.ProjectTemplateResponse;
import com.arqly.backend.dto.ProjectDtos.ProjectUpdateRequest;
import com.arqly.backend.dto.ProjectDtos.ReorderRequest;
import com.arqly.backend.dto.ProjectDtos.ResponsibleWorkloadResponse;
import com.arqly.backend.dto.ProjectDtos.StageChecklistRequest;
import com.arqly.backend.dto.ProjectDtos.StageChecklistResponse;
import com.arqly.backend.dto.ProjectDtos.StageTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.StageTemplateResponse;
import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.OriginType;
import com.arqly.backend.entity.ProgressCalculationMode;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectPhase;
import com.arqly.backend.entity.ProjectPhaseTemplate;
import com.arqly.backend.entity.ProjectService;
import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.ProjectStageChecklistItem;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStageTemplate;
import com.arqly.backend.entity.ProjectStageTemplateChecklist;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.entity.ProjectTemplate;
import com.arqly.backend.entity.ProposalItem;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ProjectPhaseRepository;
import com.arqly.backend.repository.ProjectPhaseTemplateRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectServiceRepository;
import com.arqly.backend.repository.ProjectStageChecklistItemRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.ProjectStageTemplateChecklistRepository;
import com.arqly.backend.repository.ProjectStageTemplateRepository;
import com.arqly.backend.repository.ProjectTemplateRepository;
import com.arqly.backend.repository.ProposalItemRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.FileResourceRepository;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectManagementService {
    private final ProjectRepository projectRepository;
    private final ProjectServiceRepository projectServiceRepository;
    private final ProjectTemplateRepository templateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectStageTemplateRepository stageTemplateRepository;
    private final ProjectStageTemplateChecklistRepository stageTemplateChecklistRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectStageChecklistItemRepository stageChecklistRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final ProposalRepository proposalRepository;
    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final FileResourceRepository fileResourceRepository;
    private final ActivityEventPublisher activityPublisher;

    public ProjectManagementService(ProjectRepository projectRepository,
                                    ProjectServiceRepository projectServiceRepository,
                                    ProjectTemplateRepository templateRepository,
                                    ProjectPhaseTemplateRepository phaseTemplateRepository,
                                    ProjectStageTemplateRepository stageTemplateRepository,
                                    ProjectStageTemplateChecklistRepository stageTemplateChecklistRepository,
                                    ProjectPhaseRepository phaseRepository,
                                    ProjectStageRepository stageRepository,
                                    ProjectStageChecklistItemRepository stageChecklistRepository,
                                    ProposalItemRepository proposalItemRepository,
                                    ProposalRepository proposalRepository,
                                    ClientRepository clientRepository,
                                    TenantRepository tenantRepository,
                                    TenantUserRepository tenantUserRepository,
                                    FileResourceRepository fileResourceRepository,
                                    ActivityEventPublisher activityPublisher) {
        this.projectRepository = projectRepository;
        this.projectServiceRepository = projectServiceRepository;
        this.templateRepository = templateRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.stageTemplateRepository = stageTemplateRepository;
        this.stageTemplateChecklistRepository = stageTemplateChecklistRepository;
        this.phaseRepository = phaseRepository;
        this.stageRepository = stageRepository;
        this.stageChecklistRepository = stageChecklistRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.proposalRepository = proposalRepository;
        this.clientRepository = clientRepository;
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.fileResourceRepository = fileResourceRepository;
        this.activityPublisher = activityPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ProjectSummaryResponse> list(UUID tenantId, UUID clientId, String responsible, ProjectStatus status,
                                             UUID templateId, LocalDate from, LocalDate to, Pageable pageable) {
        return projectRepository.findAll(specification(tenantId, clientId, responsible, status, templateId, from, to), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID tenantId, UUID id) {
        return toResponse(findProject(tenantId, id));
    }

    @Transactional
    public ProjectResponse createFromProposal(UUID tenantId, UUID proposalId, String username, CreateProjectFromProposalRequest request) {
        var proposal = proposalRepository.findByIdAndTenantIdAndDeletedFalse(proposalId, tenantId)
                .orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new BusinessException("Somente propostas aceitas podem virar projeto.");
        }
        if (proposal.isProjectCreated() || projectRepository.existsByProposalIdAndDeletedFalse(proposalId)) {
            throw new BusinessException("Esta proposta já possui um projeto vinculado.");
        }

        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var inheritedTemplateId = proposal.getBriefing() == null || proposal.getBriefing().getProjectTemplate() == null
                ? null
                : proposal.getBriefing().getProjectTemplate().getId();
        var selectedTemplateId = request.templateId() == null ? inheritedTemplateId : request.templateId();
        var template = selectedTemplateId == null ? null : findTemplate(tenantId, selectedTemplateId);
        var project = new Project();
        project.setTenant(tenant);
        project.setClient(proposal.getClient());
        project.setProposal(proposal);
        project.setOriginType(OriginType.PROPOSAL);
        project.setTemplate(template);
        project.setCode(nextCode(tenantId));
        project.setName(firstNotBlank(request.name(), proposal.getTitle()));
        project.setDescription(proposal.getDescription());
        project.setInternalNotes(request.internalNotes());
        project.setResponsibleUser(findTenantUserOrNull(tenantId, request.responsibleUserId()));
        project.setProjectManager(findTenantUserOrNull(tenantId, request.projectManagerId()));
        project.setLegacyResponsibleName(request.responsibleArchitect());
        project.setContractedValue(value(proposal.getTotal()));
        project.setStartDate(request.startDate());
        project.setExpectedEndDate(request.expectedEndDate());
        project.setStatus(ProjectStatus.PLANNING);
        project.setCreatedBy(username);
        project.setUpdatedBy(username);
        project = projectRepository.save(project);

        for (ProposalItem item : proposalItemRepository.findAllByProposalIdOrderByCreatedAtAsc(proposal.getId())) {
            var service = new ProjectService();
            service.setProject(project);
            service.setName(item.getServiceName());
            service.setDescription(firstNotBlank(item.getCustomDescription(), item.getServiceDescription()));
            service.setQuantity(value(item.getQuantity()));
            service.setUnit(item.getUnit());
            service.setContractedValue(value(item.getTotal()));
            projectServiceRepository.save(service);
        }
        if (template != null) {
            copyPhasesFromTemplate(tenantId, project, template, username);
        }
        proposal.setProjectCreated(true);
        publishProject(project, username, ActivityType.PROJECT_CREATED, "Projeto criado",
                "criou o projeto a partir da proposta " + proposal.getNumber());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse createManual(UUID tenantId, String username, CreateProjectManualRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var client = clientRepository.findByIdAndTenantIdAndDeletedFalse(request.clientId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        var template = request.templateId() == null ? null : findTemplate(tenantId, request.templateId());
        var project = new Project();
        project.setTenant(tenant);
        project.setClient(client);
        project.setProposal(null);
        project.setOriginType(OriginType.MANUAL);
        project.setTemplate(template);
        project.setCode(nextCode(tenantId));
        project.setName(request.name());
        project.setDescription(request.description());
        project.setInternalNotes(request.internalNotes());
        project.setResponsibleUser(findTenantUserOrNull(tenantId, request.responsibleUserId()));
        project.setProjectManager(findTenantUserOrNull(tenantId, request.projectManagerId()));
        project.setLegacyResponsibleName(request.responsibleArchitect());
        project.setContractedValue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        project.setStartDate(request.startDate());
        project.setExpectedEndDate(request.expectedEndDate());
        project.setStatus(ProjectStatus.PLANNING);
        project.setCreatedBy(username);
        project.setUpdatedBy(username);
        project = projectRepository.save(project);
        if (template != null) {
            copyPhasesFromTemplate(tenantId, project, template, username);
        }
        publishProject(project, username, ActivityType.PROJECT_CREATED, "Projeto criado", "criou o projeto manualmente");
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(UUID tenantId, UUID id, String username, ProjectUpdateRequest request) {
        var project = findProject(tenantId, id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setTemplate(request.templateId() == null ? null : findTemplate(tenantId, request.templateId()));
        project.setStartDate(request.startDate());
        project.setExpectedEndDate(request.expectedEndDate());
        project.setCompletedAt(request.completedAt());
        project.setResponsibleUser(findTenantUserOrNull(tenantId, request.responsibleUserId()));
        project.setProjectManager(findTenantUserOrNull(tenantId, request.projectManagerId()));
        project.setLegacyResponsibleName(request.responsibleArchitect());
        project.setInternalNotes(request.internalNotes());
        project.setUpdatedBy(username);
        if (request.status() != null) {
            project.setStatus(request.status());
            if (request.status() == ProjectStatus.COMPLETED && project.getCompletedAt() == null) {
                project.setCompletedAt(LocalDate.now());
                publishProject(project, username, ActivityType.PROJECT_COMPLETED, "Projeto concluído", "concluiu o projeto");
            }
        }
        publishProject(project, username, ActivityType.PROJECT_UPDATED, "Projeto atualizado", "atualizou os dados do projeto");
        return toResponse(project);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var project = findProject(tenantId, id);
        project.setDeleted(true);
        project.setDeletedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public ProjectStatsResponse stats(UUID tenantId) {
        var start = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        var end = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new ProjectStatsResponse(
                projectRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.IN_PROGRESS)
                        + projectRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.PLANNING),
                projectRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.COMPLETED),
                projectRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.ON_HOLD),
                projectRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.CANCELLED),
                projectRepository.countByTenantIdAndDeletedFalseAndCreatedAtBetween(tenantId, start, end),
                stageRepository.countByTenantIdAndStatusNotAndPlannedEndBeforeAndDeletedFalse(tenantId, ProjectStageStatus.COMPLETED, LocalDate.now()),
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.COMPLETED),
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.IN_PROGRESS),
                workload(projectRepository.countProjectsByResponsible(tenantId)),
                workload(stageRepository.countStagesByResponsible(tenantId)),
                projectRepository.countByTenantIdAndOriginTypeAndDeletedFalse(tenantId, OriginType.MANUAL),
                projectRepository.countByTenantIdAndOriginTypeAndDeletedFalse(tenantId, OriginType.PROPOSAL)
        );
    }

    @Transactional(readOnly = true)
    public Page<ProjectTemplateResponse> listTemplates(UUID tenantId, Pageable pageable) {
        return templateRepository.findAllByTenantIdAndDeletedFalse(tenantId, pageable)
                .map(template -> toTemplateResponse(tenantId, template, false));
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateResponse> templateOptions(UUID tenantId) {
        return templateRepository.findAllByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId).stream()
                .map(template -> toTemplateResponse(tenantId, template, false))
                .toList();
    }

    @Transactional
    public ProjectTemplateResponse createTemplate(UUID tenantId, ProjectTemplateRequest request) {
        if (templateRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(tenantId, request.name())) {
            throw new BusinessException("Já existe um modelo com este nome.");
        }
        var template = new ProjectTemplate();
        template.setTenant(tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado.")));
        template.setName(request.name());
        template.setDescription(request.description());
        template.setActive(request.active());
        return toTemplateResponse(tenantId, templateRepository.save(template), true);
    }

    @Transactional
    public ProjectTemplateResponse updateTemplate(UUID tenantId, UUID id, ProjectTemplateRequest request) {
        var template = findTemplate(tenantId, id);
        template.setName(request.name());
        template.setDescription(request.description());
        template.setActive(request.active());
        return toTemplateResponse(tenantId, template, true);
    }

    @Transactional
    public ProjectTemplateResponse duplicateTemplate(UUID tenantId, UUID id) {
        var source = findTemplate(tenantId, id);
        var copy = new ProjectTemplate();
        copy.setTenant(source.getTenant());
        copy.setName(source.getName() + " (cópia)");
        copy.setDescription(source.getDescription());
        copy.setActive(source.isActive());
        copy = templateRepository.save(copy);
        for (ProjectPhaseTemplate sourcePhase : phaseTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var phase = copyPhaseTemplate(sourcePhase, copy);
            phase = phaseTemplateRepository.save(phase);
            for (ProjectStageTemplate sourceStage : stageTemplateRepository.findAllByProjectPhaseTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(sourcePhase.getId(), tenantId)) {
                var stage = copyStageTemplate(sourceStage, phase);
                stage = stageTemplateRepository.save(stage);
                copyTemplateChecklist(tenantId, sourceStage, stage);
            }
        }
        return toTemplateResponse(tenantId, copy, true);
    }

    @Transactional
    public void deleteTemplate(UUID tenantId, UUID id) {
        var template = findTemplate(tenantId, id);
        template.setDeleted(true);
        template.setDeletedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<PhaseTemplateResponse> listPhaseTemplates(UUID tenantId, UUID templateId) {
        findTemplate(tenantId, templateId);
        return phaseTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(templateId, tenantId).stream()
                .map(phase -> toPhaseTemplateResponse(tenantId, phase, true))
                .toList();
    }

    @Transactional
    public PhaseTemplateResponse createPhaseTemplate(UUID tenantId, UUID templateId, PhaseTemplateRequest request) {
        var template = findTemplate(tenantId, templateId);
        var phase = new ProjectPhaseTemplate();
        phase.setTenant(template.getTenant());
        phase.setProjectTemplate(template);
        fillPhaseTemplate(phase, request);
        return toPhaseTemplateResponse(tenantId, phaseTemplateRepository.save(phase), true);
    }

    @Transactional
    public PhaseTemplateResponse updatePhaseTemplate(UUID tenantId, UUID templateId, UUID phaseId, PhaseTemplateRequest request) {
        findTemplate(tenantId, templateId);
        var phase = findPhaseTemplate(tenantId, phaseId);
        if (!phase.getProjectTemplate().getId().equals(templateId)) {
            throw new NotFoundException("Fase do modelo não encontrada.");
        }
        fillPhaseTemplate(phase, request);
        return toPhaseTemplateResponse(tenantId, phase, true);
    }

    @Transactional
    public PhaseTemplateResponse duplicatePhaseTemplate(UUID tenantId, UUID templateId, UUID phaseId) {
        findTemplate(tenantId, templateId);
        var source = findPhaseTemplate(tenantId, phaseId);
        var copy = copyPhaseTemplate(source, source.getProjectTemplate());
        copy.setName(source.getName() + " (cópia)");
        copy.setOrder(source.getOrder() + 1);
        copy = phaseTemplateRepository.save(copy);
        for (ProjectStageTemplate sourceStage : stageTemplateRepository.findAllByProjectPhaseTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var stage = copyStageTemplate(sourceStage, copy);
            stage = stageTemplateRepository.save(stage);
            copyTemplateChecklist(tenantId, sourceStage, stage);
        }
        return toPhaseTemplateResponse(tenantId, copy, true);
    }

    @Transactional
    public void deletePhaseTemplate(UUID tenantId, UUID templateId, UUID phaseId) {
        findTemplate(tenantId, templateId);
        var phase = findPhaseTemplate(tenantId, phaseId);
        if (!phase.getProjectTemplate().getId().equals(templateId)) {
            throw new NotFoundException("Fase do modelo não encontrada.");
        }
        phase.setDeleted(true);
        phase.setDeletedAt(Instant.now());
        stageTemplateRepository.findAllByProjectPhaseTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phaseId, tenantId)
                .forEach(stage -> {
                    stage.setDeleted(true);
                    stage.setDeletedAt(Instant.now());
                });
    }

    @Transactional
    public List<PhaseTemplateResponse> reorderPhaseTemplates(UUID tenantId, UUID templateId, ReorderRequest request) {
        findTemplate(tenantId, templateId);
        for (var item : request.items()) {
            var phase = findPhaseTemplate(tenantId, item.id());
            if (phase.getProjectTemplate().getId().equals(templateId)) {
                phase.setOrder(item.order());
            }
        }
        return listPhaseTemplates(tenantId, templateId);
    }

    @Transactional
    public StageTemplateResponse createStageTemplate(UUID tenantId, UUID templateId, UUID phaseId, StageTemplateRequest request) {
        findTemplate(tenantId, templateId);
        var phase = findPhaseTemplate(tenantId, phaseId);
        var stage = new ProjectStageTemplate();
        stage.setTenant(phase.getTenant());
        stage.setProjectPhaseTemplate(phase);
        fillStageTemplate(stage, request);
        stage = stageTemplateRepository.save(stage);
        replaceStageTemplateChecklist(tenantId, stage, request.checklist());
        return toStageTemplateResponse(tenantId, stage);
    }

    @Transactional
    public StageTemplateResponse updateStageTemplate(UUID tenantId, UUID templateId, UUID phaseId, UUID stageId, StageTemplateRequest request) {
        findTemplate(tenantId, templateId);
        findPhaseTemplate(tenantId, phaseId);
        var stage = findStageTemplate(tenantId, stageId);
        if (!stage.getProjectPhaseTemplate().getId().equals(phaseId)) {
            throw new NotFoundException("Etapa do modelo não encontrada.");
        }
        fillStageTemplate(stage, request);
        replaceStageTemplateChecklist(tenantId, stage, request.checklist());
        return toStageTemplateResponse(tenantId, stage);
    }

    @Transactional
    public StageTemplateResponse duplicateStageTemplate(UUID tenantId, UUID templateId, UUID phaseId, UUID stageId) {
        findTemplate(tenantId, templateId);
        var phase = findPhaseTemplate(tenantId, phaseId);
        var source = findStageTemplate(tenantId, stageId);
        var copy = copyStageTemplate(source, phase);
        copy.setName(source.getName() + " (cópia)");
        copy.setOrder(source.getOrder() + 1);
        copy = stageTemplateRepository.save(copy);
        copyTemplateChecklist(tenantId, source, copy);
        return toStageTemplateResponse(tenantId, copy);
    }

    @Transactional
    public void deleteStageTemplate(UUID tenantId, UUID templateId, UUID phaseId, UUID stageId) {
        findTemplate(tenantId, templateId);
        findPhaseTemplate(tenantId, phaseId);
        var stage = findStageTemplate(tenantId, stageId);
        stage.setDeleted(true);
        stage.setDeletedAt(Instant.now());
    }

    @Transactional
    public List<PhaseTemplateResponse> reorderStageTemplates(UUID tenantId, UUID templateId, UUID phaseId, ReorderRequest request) {
        findTemplate(tenantId, templateId);
        findPhaseTemplate(tenantId, phaseId);
        for (var item : request.items()) {
            var stage = findStageTemplate(tenantId, item.id());
            var targetPhase = item.projectPhaseId() == null ? findPhaseTemplate(tenantId, phaseId) : findPhaseTemplate(tenantId, item.projectPhaseId());
            stage.setProjectPhaseTemplate(targetPhase);
            stage.setOrder(item.order());
        }
        return listPhaseTemplates(tenantId, templateId);
    }

    @Transactional(readOnly = true)
    public List<ProjectPhaseResponse> listPhases(UUID tenantId, UUID projectId) {
        findProject(tenantId, projectId);
        return phaseRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(projectId, tenantId).stream()
                .map(phase -> toProjectPhaseResponse(tenantId, phase, true))
                .toList();
    }

    @Transactional
    public ProjectPhaseResponse createPhase(UUID tenantId, UUID projectId, String username, ProjectPhaseRequest request) {
        var project = findProject(tenantId, projectId);
        var phase = new ProjectPhase();
        phase.setTenant(project.getTenant());
        phase.setProject(project);
        fillProjectPhase(phase, request, username);
        phase = phaseRepository.save(phase);
        activityPublisher.publish(tenantId, projectId, phase.getId(), null, null, username,
                ActivityType.PHASE_CREATED, "Fase criada", "criou a fase " + phase.getName());
        return toProjectPhaseResponse(tenantId, phase, true);
    }

    @Transactional
    public ProjectPhaseResponse updatePhase(UUID tenantId, UUID projectId, UUID phaseId, String username, ProjectPhaseRequest request) {
        findProject(tenantId, projectId);
        var phase = findPhase(tenantId, phaseId);
        if (!phase.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Fase não encontrada.");
        }
        fillProjectPhase(phase, request, username);
        activityPublisher.publish(tenantId, projectId, phase.getId(), null, null, username,
                ActivityType.PHASE_UPDATED, "Fase atualizada", "atualizou a fase " + phase.getName());
        return toProjectPhaseResponse(tenantId, phase, true);
    }

    @Transactional
    public ProjectPhaseResponse duplicatePhase(UUID tenantId, UUID projectId, UUID phaseId, String username) {
        findProject(tenantId, projectId);
        var source = findPhase(tenantId, phaseId);
        var copy = new ProjectPhase();
        copy.setTenant(source.getTenant());
        copy.setProject(source.getProject());
        copy.setPhaseTemplate(source.getPhaseTemplate());
        copy.setName(source.getName() + " (cópia)");
        copy.setDescription(source.getDescription());
        copy.setOrder(source.getOrder() + 1);
        copy.setColor(source.getColor());
        copy.setIcon(source.getIcon());
        copy.setCreatedBy(username);
        copy.setUpdatedBy(username);
        copy = phaseRepository.save(copy);
        for (ProjectStage sourceStage : stageRepository.findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var stage = copyStage(sourceStage, copy, username);
            stage = stageRepository.save(stage);
        copyProjectChecklist(tenantId, sourceStage, stage);
        }
        recalculatePhase(copy.getTenant().getId(), copy.getId());
        activityPublisher.publish(tenantId, projectId, copy.getId(), null, null, username,
                ActivityType.PHASE_CREATED, "Fase duplicada", "duplicou a fase " + source.getName());
        return toProjectPhaseResponse(tenantId, copy, true);
    }

    @Transactional
    public void deletePhase(UUID tenantId, UUID projectId, UUID phaseId) {
        findProject(tenantId, projectId);
        var phase = findPhase(tenantId, phaseId);
        if (!phase.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Fase não encontrada.");
        }
        phase.setDeleted(true);
        phase.setDeletedAt(Instant.now());
        stageRepository.findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phaseId, tenantId)
                .forEach(stage -> {
                    stage.setDeleted(true);
                    stage.setDeletedAt(Instant.now());
                });
    }

    @Transactional
    public List<ProjectPhaseResponse> reorderPhases(UUID tenantId, UUID projectId, ReorderRequest request) {
        findProject(tenantId, projectId);
        for (var item : request.items()) {
            var phase = findPhase(tenantId, item.id());
            if (phase.getProject().getId().equals(projectId)) {
                phase.setOrder(item.order());
            }
        }
        return listPhases(tenantId, projectId);
    }

    @Transactional
    public ProjectStageResponse createStage(UUID tenantId, UUID projectId, UUID phaseId, String username, ProjectStageRequest request) {
        findProject(tenantId, projectId);
        var phase = findPhase(tenantId, phaseId);
        if (!phase.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Fase do projeto não encontrada.");
        }
        var stage = new ProjectStage();
        stage.setTenant(phase.getTenant());
        stage.setProjectPhase(phase);
        fillProjectStage(tenantId, stage, phaseId, request, username);
        stage = stageRepository.save(stage);
        replaceProjectChecklist(tenantId, stage, request.checklist());
        recalculatePhase(tenantId, phase.getId());
        activityPublisher.publish(tenantId, projectId, phaseId, stage.getId(), null, username,
                ActivityType.STAGE_CREATED, "Etapa criada", "criou a etapa " + stage.getName());
        return toProjectStageResponse(tenantId, stage);
    }

    @Transactional
    public ProjectStageResponse updateStage(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId, String username, ProjectStageRequest request) {
        findProject(tenantId, projectId);
        var stage = findStage(tenantId, stageId);
        var oldPhaseId = stage.getProjectPhase().getId();
        fillProjectStage(tenantId, stage, phaseId, request, username);
        replaceProjectChecklist(tenantId, stage, request.checklist());
        recalculatePhase(tenantId, oldPhaseId);
        recalculatePhase(tenantId, stage.getProjectPhase().getId());
        activityPublisher.publish(tenantId, projectId, stage.getProjectPhase().getId(), stage.getId(), null, username,
                ActivityType.STAGE_UPDATED, "Etapa atualizada", "atualizou a etapa " + stage.getName());
        return toProjectStageResponse(tenantId, stage);
    }

    @Transactional
    public ProjectStageResponse duplicateStage(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId, String username) {
        findProject(tenantId, projectId);
        var source = findStage(tenantId, stageId);
        if (!source.getProjectPhase().getId().equals(phaseId)) {
            throw new NotFoundException("Etapa não encontrada nesta fase.");
        }
        var copy = copyStage(source, source.getProjectPhase(), username);
        copy.setName(source.getName() + " (cópia)");
        copy.setOrder(source.getOrder() + 1);
        copy = stageRepository.save(copy);
        copyProjectChecklist(tenantId, source, copy);
        recalculatePhase(tenantId, copy.getProjectPhase().getId());
        activityPublisher.publish(tenantId, projectId, copy.getProjectPhase().getId(), copy.getId(), null, username,
                ActivityType.STAGE_CREATED, "Etapa duplicada", "duplicou a etapa " + source.getName());
        return toProjectStageResponse(tenantId, copy);
    }

    @Transactional
    public void deleteStage(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId) {
        findProject(tenantId, projectId);
        var stage = findStage(tenantId, stageId);
        if (!stage.getProjectPhase().getId().equals(phaseId)) {
            throw new NotFoundException("Etapa não encontrada nesta fase.");
        }
        var currentPhaseId = stage.getProjectPhase().getId();
        stage.setDeleted(true);
        stage.setDeletedAt(Instant.now());
        recalculatePhase(tenantId, currentPhaseId);
    }

    @Transactional
    public List<ProjectPhaseResponse> reorderStages(UUID tenantId, UUID projectId, ReorderRequest request) {
        findProject(tenantId, projectId);
        var affected = new ArrayList<UUID>();
        for (var item : request.items()) {
            var stage = findStage(tenantId, item.id());
            var targetPhase = item.projectPhaseId() == null ? stage.getProjectPhase() : findPhase(tenantId, item.projectPhaseId());
            affected.add(stage.getProjectPhase().getId());
            stage.setProjectPhase(targetPhase);
            stage.setOrder(item.order());
            affected.add(targetPhase.getId());
        }
        affected.stream().distinct().forEach(id -> recalculatePhase(tenantId, id));
        return listPhases(tenantId, projectId);
    }

    private Specification<Project> specification(UUID tenantId, UUID clientId, String responsible, ProjectStatus status,
                                                 UUID templateId, LocalDate from, LocalDate to) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (clientId != null) predicates.add(builder.equal(root.get("client").get("id"), clientId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (templateId != null) predicates.add(builder.equal(root.get("template").get("id"), templateId));
            if (responsible != null && !responsible.isBlank()) {
                var term = "%" + responsible.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("legacyResponsibleName")), term),
                        builder.like(builder.lower(root.get("responsibleUser").get("name")), term),
                        builder.like(builder.lower(root.get("responsibleUser").get("email")), term)
                ));
            }
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Project findProject(UUID tenantId, UUID id) {
        return projectRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
    }

    private ProjectTemplate findTemplate(UUID tenantId, UUID id) {
        return templateRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Modelo não encontrado."));
    }

    private TenantUser findTenantUserOrNull(UUID tenantId, UUID id) {
        if (id == null) return null;
        return tenantUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário responsável não encontrado."));
    }

    private ProjectPhaseTemplate findPhaseTemplate(UUID tenantId, UUID id) {
        return phaseTemplateRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fase do modelo não encontrada."));
    }

    private ProjectStageTemplate findStageTemplate(UUID tenantId, UUID id) {
        return stageTemplateRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Etapa do modelo não encontrada."));
    }

    private ProjectPhase findPhase(UUID tenantId, UUID id) {
        return phaseRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fase não encontrada."));
    }

    private ProjectStage findStage(UUID tenantId, UUID id) {
        return stageRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Etapa não encontrada."));
    }

    private String nextCode(UUID tenantId) {
        var prefix = "PRJ-" + Year.now().getValue() + "-";
        return projectRepository.findTopByTenantIdAndCodeStartingWithOrderByCodeDesc(tenantId, prefix)
                .map(Project::getCode)
                .map(code -> code.substring(code.lastIndexOf('-') + 1))
                .map(Integer::parseInt)
                .map(value -> value + 1)
                .map(value -> prefix + "%06d".formatted(value))
                .orElse(prefix + "000001");
    }

    private ProjectSummaryResponse toSummary(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getCode(), project.getName(), project.getClient().getId(),
                displayName(project.getClient()),
                project.getProposal() == null ? null : project.getProposal().getId(),
                project.getProposal() == null ? null : project.getProposal().getNumber(),
                project.getOriginType(),
                project.getTemplate() == null ? null : project.getTemplate().getId(),
                project.getTemplate() == null ? null : project.getTemplate().getName(),
                project.getResponsibleUser() == null ? null : project.getResponsibleUser().getId(),
                project.getResponsibleUser() == null ? null : project.getResponsibleUser().getName(),
                project.getProjectManager() == null ? null : project.getProjectManager().getId(),
                project.getProjectManager() == null ? null : project.getProjectManager().getName(),
                responsibleName(project), project.getStatus(), project.getExpectedEndDate(),
                project.getContractedValue(), projectProgress(project.getTenant().getId(), project.getId()), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectResponse toResponse(Project project) {
        var services = projectServiceRepository.findAllByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(service -> new ProjectServiceResponse(service.getId(), service.getName(), service.getDescription(),
                        service.getQuantity(), service.getUnit(), service.getContractedValue()))
                .toList();
        return new ProjectResponse(project.getId(), project.getCode(), project.getName(), project.getDescription(),
                project.getClient().getId(), displayName(project.getClient()),
                project.getProposal() == null ? null : project.getProposal().getId(),
                project.getProposal() == null ? null : project.getProposal().getNumber(),
                project.getOriginType(),
                project.getTemplate() == null ? null : project.getTemplate().getId(),
                project.getTemplate() == null ? null : project.getTemplate().getName(),
                project.getResponsibleUser() == null ? null : project.getResponsibleUser().getId(),
                project.getResponsibleUser() == null ? null : project.getResponsibleUser().getName(),
                project.getProjectManager() == null ? null : project.getProjectManager().getId(),
                project.getProjectManager() == null ? null : project.getProjectManager().getName(),
                responsibleName(project), project.getStatus(), project.getStartDate(), project.getExpectedEndDate(),
                project.getCompletedAt(), project.getContractedValue(), projectProgress(project.getTenant().getId(), project.getId()),
                project.getInternalNotes(), project.getCreatedBy(), project.getUpdatedBy(), services,
                listPhases(project.getTenant().getId(), project.getId()), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectTemplateResponse toTemplateResponse(UUID tenantId, ProjectTemplate template, boolean includePhases) {
        var phases = includePhases ? listPhaseTemplates(tenantId, template.getId()) : List.<PhaseTemplateResponse>of();
        return new ProjectTemplateResponse(template.getId(), template.getName(), template.getDescription(),
                template.isActive(),
                phaseTemplateRepository.countByProjectTemplateIdAndTenantIdAndDeletedFalse(template.getId(), tenantId),
                stageTemplateRepository.countByProjectPhaseTemplateProjectTemplateIdAndTenantIdAndDeletedFalse(template.getId(), tenantId),
                projectRepository.countByTenantIdAndTemplateIdAndDeletedFalse(tenantId, template.getId()),
                phases, template.getCreatedAt(), template.getUpdatedAt());
    }

    private PhaseTemplateResponse toPhaseTemplateResponse(UUID tenantId, ProjectPhaseTemplate phase, boolean includeStages) {
        var stages = includeStages
                ? stageTemplateRepository.findAllByProjectPhaseTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phase.getId(), tenantId).stream()
                    .map(stage -> toStageTemplateResponse(tenantId, stage))
                    .toList()
                : List.<StageTemplateResponse>of();
        return new PhaseTemplateResponse(phase.getId(), phase.getName(), phase.getDescription(), phase.getOrder(),
                phase.getColor(), phase.getIcon(), phase.isActive(),
                stageTemplateRepository.countByProjectPhaseTemplateIdAndTenantIdAndDeletedFalse(phase.getId(), tenantId),
                stages, phase.getCreatedAt(), phase.getUpdatedAt());
    }

    private StageTemplateResponse toStageTemplateResponse(UUID tenantId, ProjectStageTemplate stage) {
        var checklist = stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId).stream()
                .map(item -> new StageChecklistResponse(item.getId(), item.getTitle(), item.getOrder(), false, null))
                .toList();
        return new StageTemplateResponse(stage.getId(), stage.getProjectPhaseTemplate().getId(), stage.getName(),
                stage.getDescription(), stage.getOrder(), stage.getWeightPercentage(), stage.getProgressCalculationMode(),
                stage.isActive(), checklist, stage.getCreatedAt(), stage.getUpdatedAt());
    }

    private ProjectPhaseResponse toProjectPhaseResponse(UUID tenantId, ProjectPhase phase, boolean includeStages) {
        var stages = includeStages
                ? stageRepository.findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phase.getId(), tenantId).stream()
                    .map(stage -> toProjectStageResponse(tenantId, stage))
                    .toList()
                : List.<ProjectStageResponse>of();
        return new ProjectPhaseResponse(phase.getId(), phase.getPhaseTemplate() == null ? null : phase.getPhaseTemplate().getId(),
                phase.getName(), phase.getDescription(), phase.getOrder(), phase.getColor(), phase.getIcon(), phase.getStatus(),
                phase.getCompletionPercentage(), phase.getNotes(), stages, phase.getCreatedAt(), phase.getUpdatedAt());
    }

    private ProjectStageResponse toProjectStageResponse(UUID tenantId, ProjectStage stage) {
        var checklist = stageChecklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId).stream()
                .map(item -> new StageChecklistResponse(item.getId(), item.getDescription(), item.getOrder(), item.isCompleted(), item.getCompletedAt()))
                .toList();
        return new ProjectStageResponse(stage.getId(), stage.getProjectPhase().getId(), stage.getTemplate() == null ? null : stage.getTemplate().getId(),
                stage.getDependsOnStage() == null ? null : stage.getDependsOnStage().getId(), stage.getName(), stage.getDescription(),
                stage.getOrder(), stage.getStatus(), stage.getPlannedStart(), stage.getPlannedEnd(), stage.getActualStart(),
                stage.getActualEnd(), stage.getCompletionPercentage(), stage.getWeightPercentage(), stage.getProgressCalculationMode(),
                stage.getResponsibleUser() == null ? null : stage.getResponsibleUser().getId(), responsibleName(stage),
                responsibleName(stage), stage.getNotes(),
                fileResourceRepository.countByTenantIdAndOwnerTypeAndOwnerIdAndStatusNot(
                        tenantId, com.arqly.backend.entity.FileOwnerType.PROJECT_STAGE, stage.getId(),
                        com.arqly.backend.entity.FileResourceStatus.DELETED),
                0, checklist, stage.getCreatedAt(), stage.getUpdatedAt());
    }

    private void fillPhaseTemplate(ProjectPhaseTemplate phase, PhaseTemplateRequest request) {
        phase.setName(request.name());
        phase.setDescription(request.description());
        phase.setOrder(request.order());
        phase.setColor(request.color());
        phase.setIcon(request.icon());
        phase.setActive(request.active());
    }

    private void fillStageTemplate(ProjectStageTemplate stage, StageTemplateRequest request) {
        stage.setName(request.name());
        stage.setDescription(request.description());
        stage.setOrder(request.order());
        stage.setWeightPercentage(scale(request.weightPercentage()));
        stage.setProgressCalculationMode(request.progressCalculationMode() == null ? ProgressCalculationMode.MANUAL : request.progressCalculationMode());
        stage.setActive(request.active());
    }

    private void fillProjectPhase(ProjectPhase phase, ProjectPhaseRequest request, String username) {
        phase.setName(request.name());
        phase.setDescription(request.description());
        phase.setOrder(request.order());
        phase.setColor(request.color());
        phase.setIcon(request.icon());
        phase.setStatus(request.status() == null ? ProjectStageStatus.NOT_STARTED : request.status());
        phase.setNotes(request.notes());
        phase.setUpdatedBy(username);
        if (phase.getCreatedBy() == null) {
            phase.setCreatedBy(username);
        }
    }

    private void fillProjectStage(UUID tenantId, ProjectStage stage, UUID phaseId, ProjectStageRequest request, String username) {
        var requestedPhaseId = request.projectPhaseId() == null ? phaseId : request.projectPhaseId();
        var phase = findPhase(tenantId, requestedPhaseId);
        stage.setProjectPhase(phase);
        stage.setName(request.name());
        stage.setDescription(request.description());
        stage.setOrder(request.order());
        stage.setStatus(request.status() == null ? ProjectStageStatus.NOT_STARTED : request.status());
        stage.setPlannedStart(request.plannedStart());
        stage.setPlannedEnd(request.plannedEnd());
        stage.setActualStart(request.actualStart());
        stage.setActualEnd(request.actualEnd());
        stage.setCompletionPercentage(scale(request.completionPercentage()));
        stage.setWeightPercentage(scale(request.weightPercentage()));
        stage.setProgressCalculationMode(request.progressCalculationMode() == null ? ProgressCalculationMode.MANUAL : request.progressCalculationMode());
        stage.setResponsibleUser(findTenantUserOrNull(tenantId, request.responsibleUserId()));
        stage.setLegacyResponsibleName(request.responsible());
        stage.setNotes(request.notes());
        stage.setUpdatedBy(username);
        if (stage.getCreatedBy() == null) {
            stage.setCreatedBy(username);
        }
        stage.setDependsOnStage(request.dependsOnStageId() == null ? null : findStage(tenantId, request.dependsOnStageId()));
    }

    private ProjectPhaseTemplate copyPhaseTemplate(ProjectPhaseTemplate source, ProjectTemplate targetTemplate) {
        var phase = new ProjectPhaseTemplate();
        phase.setTenant(source.getTenant());
        phase.setProjectTemplate(targetTemplate);
        phase.setName(source.getName());
        phase.setDescription(source.getDescription());
        phase.setOrder(source.getOrder());
        phase.setColor(source.getColor());
        phase.setIcon(source.getIcon());
        phase.setActive(source.isActive());
        return phase;
    }

    private ProjectStageTemplate copyStageTemplate(ProjectStageTemplate source, ProjectPhaseTemplate targetPhase) {
        var stage = new ProjectStageTemplate();
        stage.setTenant(source.getTenant());
        stage.setProjectPhaseTemplate(targetPhase);
        stage.setName(source.getName());
        stage.setDescription(source.getDescription());
        stage.setOrder(source.getOrder());
        stage.setWeightPercentage(source.getWeightPercentage());
        stage.setProgressCalculationMode(source.getProgressCalculationMode());
        stage.setActive(source.isActive());
        return stage;
    }

    private ProjectStage copyStage(ProjectStage source, ProjectPhase targetPhase, String username) {
        var stage = new ProjectStage();
        stage.setTenant(source.getTenant());
        stage.setProjectPhase(targetPhase);
        stage.setTemplate(source.getTemplate());
        stage.setName(source.getName());
        stage.setDescription(source.getDescription());
        stage.setOrder(source.getOrder());
        stage.setWeightPercentage(source.getWeightPercentage());
        stage.setProgressCalculationMode(source.getProgressCalculationMode());
        stage.setCompletionPercentage(BigDecimal.ZERO);
        stage.setStatus(ProjectStageStatus.NOT_STARTED);
        stage.setResponsibleUser(source.getResponsibleUser());
        stage.setLegacyResponsibleName(source.getLegacyResponsibleName());
        stage.setCreatedBy(username);
        stage.setUpdatedBy(username);
        return stage;
    }

    private void copyTemplateChecklist(UUID tenantId, ProjectStageTemplate source, ProjectStageTemplate target) {
        for (ProjectStageTemplateChecklist item : stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var checklist = new ProjectStageTemplateChecklist();
            checklist.setTenant(target.getTenant());
            checklist.setStageTemplate(target);
            checklist.setTitle(item.getTitle());
            checklist.setOrder(item.getOrder());
            stageTemplateChecklistRepository.save(checklist);
        }
    }

    private void copyProjectChecklist(UUID tenantId, ProjectStage source, ProjectStage target) {
        for (ProjectStageChecklistItem item : stageChecklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var checklist = new ProjectStageChecklistItem();
            checklist.setTenant(target.getTenant());
            checklist.setStage(target);
            checklist.setDescription(item.getDescription());
            checklist.setOrder(item.getOrder());
            checklist.setCompleted(item.isCompleted());
            checklist.setCompletedAt(item.getCompletedAt());
            checklist.setCompletedBy(item.getCompletedBy());
            checklist.setNotes(item.getNotes());
            stageChecklistRepository.save(checklist);
        }
    }

    private void replaceStageTemplateChecklist(UUID tenantId, ProjectStageTemplate stage, List<StageChecklistRequest> checklist) {
        stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId)
                .forEach(item -> { item.setDeleted(true); item.setDeletedAt(Instant.now()); });
        if (checklist == null) return;
        for (StageChecklistRequest request : checklist) {
            var item = new ProjectStageTemplateChecklist();
            item.setTenant(stage.getTenant());
            item.setStageTemplate(stage);
            item.setTitle(request.title());
            item.setOrder(request.order());
            stageTemplateChecklistRepository.save(item);
        }
    }

    private void replaceProjectChecklist(UUID tenantId, ProjectStage stage, List<StageChecklistRequest> checklist) {
        stageChecklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId)
                .forEach(item -> { item.setDeleted(true); item.setDeletedAt(Instant.now()); });
        if (checklist == null) return;
        for (StageChecklistRequest request : checklist) {
            var item = new ProjectStageChecklistItem();
            item.setTenant(stage.getTenant());
            item.setStage(stage);
            item.setDescription(request.title());
            item.setOrder(request.order());
            item.setCompleted(request.completed());
            item.setCompletedAt(request.completed() ? Instant.now() : null);
            stageChecklistRepository.save(item);
        }
        applyChecklistProgress(tenantId, stage);
    }

    private void copyPhasesFromTemplate(UUID tenantId, Project project, ProjectTemplate template, String username) {
        for (ProjectPhaseTemplate phaseTemplate : phaseTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(template.getId(), tenantId)) {
            var phase = new ProjectPhase();
            phase.setTenant(project.getTenant());
            phase.setProject(project);
            phase.setPhaseTemplate(phaseTemplate);
            phase.setName(phaseTemplate.getName());
            phase.setDescription(phaseTemplate.getDescription());
            phase.setOrder(phaseTemplate.getOrder());
            phase.setColor(phaseTemplate.getColor());
            phase.setIcon(phaseTemplate.getIcon());
            phase.setCreatedBy(username);
            phase.setUpdatedBy(username);
            phase = phaseRepository.save(phase);
            for (ProjectStageTemplate stageTemplate : stageTemplateRepository.findAllByProjectPhaseTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(phaseTemplate.getId(), tenantId)) {
                var stage = new ProjectStage();
                stage.setTenant(project.getTenant());
                stage.setProjectPhase(phase);
                stage.setTemplate(stageTemplate);
                stage.setName(stageTemplate.getName());
                stage.setDescription(stageTemplate.getDescription());
                stage.setOrder(stageTemplate.getOrder());
                stage.setWeightPercentage(stageTemplate.getWeightPercentage());
                stage.setProgressCalculationMode(stageTemplate.getProgressCalculationMode());
                stage.setCreatedBy(username);
                stage.setUpdatedBy(username);
                stage = stageRepository.save(stage);
                for (ProjectStageTemplateChecklist templateItem : stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stageTemplate.getId(), tenantId)) {
                    var checklist = new ProjectStageChecklistItem();
                    checklist.setTenant(project.getTenant());
                    checklist.setStage(stage);
                    checklist.setDescription(templateItem.getTitle());
                    checklist.setOrder(templateItem.getOrder());
                    stageChecklistRepository.save(checklist);
                }
            }
            recalculatePhase(tenantId, phase.getId());
        }
    }

    private void recalculatePhase(UUID tenantId, UUID phaseId) {
        var phase = findPhase(tenantId, phaseId);
        phase.setCompletionPercentage(phaseProgress(tenantId, phaseId));
        var progress = phase.getCompletionPercentage().intValue();
        if (progress >= 100) {
            phase.setStatus(ProjectStageStatus.COMPLETED);
        } else if (progress > 0 && phase.getStatus() == ProjectStageStatus.NOT_STARTED) {
            phase.setStatus(ProjectStageStatus.IN_PROGRESS);
        }
    }

    private void applyChecklistProgress(UUID tenantId, ProjectStage stage) {
        if (stage.getProgressCalculationMode() != ProgressCalculationMode.CHECKLIST) {
            return;
        }
        var total = stageChecklistRepository.countByStageIdAndTenantIdAndDeletedFalse(stage.getId(), tenantId);
        if (total == 0) {
            stage.setCompletionPercentage(BigDecimal.ZERO);
            return;
        }
        var completed = stageChecklistRepository.countByStageIdAndTenantIdAndCompletedTrueAndDeletedFalse(stage.getId(), tenantId);
        var progress = BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        stage.setCompletionPercentage(progress);
        if (progress.compareTo(BigDecimal.valueOf(100)) >= 0) {
            stage.setStatus(ProjectStageStatus.COMPLETED);
            if (stage.getActualEnd() == null) {
                stage.setActualEnd(LocalDate.now());
            }
        } else if (stage.getStatus() == ProjectStageStatus.COMPLETED) {
            stage.setStatus(ProjectStageStatus.IN_PROGRESS);
            stage.setActualEnd(null);
        }
    }

    private BigDecimal projectProgress(UUID tenantId, UUID projectId) {
        var phases = phaseRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(projectId, tenantId);
        if (phases.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        var total = phases.stream()
                .map(ProjectPhase::getCompletionPercentage)
                .map(this::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(phases.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal phaseProgress(UUID tenantId, UUID phaseId) {
        var stages = stageRepository.findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(phaseId, tenantId);
        if (stages.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        var total = stages.stream()
                .map(ProjectStage::getCompletionPercentage)
                .map(this::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(stages.size()), 2, RoundingMode.HALF_UP);
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }

    private String responsibleName(Project project) {
        if (project.getResponsibleUser() != null) return project.getResponsibleUser().getName();
        if (project.getLegacyResponsibleName() != null && !project.getLegacyResponsibleName().isBlank()) return project.getLegacyResponsibleName();
        return project.getResponsibleArchitect();
    }

    private String responsibleName(ProjectStage stage) {
        if (stage.getResponsibleUser() != null) return stage.getResponsibleUser().getName();
        if (stage.getLegacyResponsibleName() != null && !stage.getLegacyResponsibleName().isBlank()) return stage.getLegacyResponsibleName();
        var projectResponsible = stage.getProjectPhase().getProject().getResponsibleUser();
        if (projectResponsible != null) return projectResponsible.getName();
        return stage.getResponsible();
    }

    private List<ResponsibleWorkloadResponse> workload(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ResponsibleWorkloadResponse((UUID) row[0], row[1] == null ? "Sem responsável" : (String) row[1], ((Number) row[2]).longValue()))
                .toList();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value(value).max(BigDecimal.ZERO).min(new BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP);
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second;
    }

    private void publishProject(Project project, String actor, ActivityType type, String title, String description) {
        activityPublisher.publish(project.getTenant().getId(), project.getId(), null, null, null, actor, type, title, description);
    }
}
