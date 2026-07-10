package com.arqly.backend.service;

import com.arqly.backend.dto.ProjectDtos.CreateProjectFromProposalRequest;
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
import com.arqly.backend.dto.ProjectDtos.StageChecklistRequest;
import com.arqly.backend.dto.ProjectDtos.StageChecklistResponse;
import com.arqly.backend.dto.ProjectDtos.StageTemplateRequest;
import com.arqly.backend.dto.ProjectDtos.StageTemplateResponse;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectService;
import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.ProjectStageChecklist;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStageTemplate;
import com.arqly.backend.entity.ProjectStageTemplateChecklist;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.entity.ProjectTemplate;
import com.arqly.backend.entity.ProposalItem;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectServiceRepository;
import com.arqly.backend.repository.ProjectStageChecklistRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.ProjectStageTemplateChecklistRepository;
import com.arqly.backend.repository.ProjectStageTemplateRepository;
import com.arqly.backend.repository.ProjectTemplateRepository;
import com.arqly.backend.repository.ProposalItemRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantRepository;
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
    private final ProjectStageTemplateRepository stageTemplateRepository;
    private final ProjectStageTemplateChecklistRepository stageTemplateChecklistRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectStageChecklistRepository stageChecklistRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final ProposalRepository proposalRepository;
    private final TenantRepository tenantRepository;

    public ProjectManagementService(ProjectRepository projectRepository,
                                    ProjectServiceRepository projectServiceRepository,
                                    ProjectTemplateRepository templateRepository,
                                    ProjectStageTemplateRepository stageTemplateRepository,
                                    ProjectStageTemplateChecklistRepository stageTemplateChecklistRepository,
                                    ProjectStageRepository stageRepository,
                                    ProjectStageChecklistRepository stageChecklistRepository,
                                    ProposalItemRepository proposalItemRepository,
                                    ProposalRepository proposalRepository,
                                    TenantRepository tenantRepository) {
        this.projectRepository = projectRepository;
        this.projectServiceRepository = projectServiceRepository;
        this.templateRepository = templateRepository;
        this.stageTemplateRepository = stageTemplateRepository;
        this.stageTemplateChecklistRepository = stageTemplateChecklistRepository;
        this.stageRepository = stageRepository;
        this.stageChecklistRepository = stageChecklistRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.proposalRepository = proposalRepository;
        this.tenantRepository = tenantRepository;
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
        var template = request.templateId() == null ? null : findTemplate(tenantId, request.templateId());
        var project = new Project();
        project.setTenant(tenant);
        project.setClient(proposal.getClient());
        project.setProposal(proposal);
        project.setTemplate(template);
        project.setCode(nextCode(tenantId));
        project.setName(request.name());
        project.setDescription(proposal.getDescription());
        project.setInternalNotes(request.internalNotes());
        project.setResponsibleArchitect(request.responsibleArchitect());
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
            project.getServices().add(service);
        }
        if (template != null) {
            copyStagesFromTemplate(tenantId, project, template, username);
        }
        proposal.setProjectCreated(true);
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
        project.setResponsibleArchitect(request.responsibleArchitect());
        project.setInternalNotes(request.internalNotes());
        project.setUpdatedBy(username);
        if (request.status() != null) {
            project.setStatus(request.status());
            if (request.status() == ProjectStatus.COMPLETED && project.getCompletedAt() == null) {
                project.setCompletedAt(LocalDate.now());
            }
        }
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
                stageRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.IN_PROGRESS)
        );
    }

    @Transactional(readOnly = true)
    public Page<ProjectTemplateResponse> listTemplates(UUID tenantId, Pageable pageable) {
        return templateRepository.findAllByTenantIdAndDeletedFalse(tenantId, pageable)
                .map(template -> toTemplateResponse(tenantId, template));
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateResponse> templateOptions(UUID tenantId) {
        return templateRepository.findAllByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId).stream()
                .map(template -> toTemplateResponse(tenantId, template))
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
        return toTemplateResponse(tenantId, templateRepository.save(template));
    }

    @Transactional
    public ProjectTemplateResponse updateTemplate(UUID tenantId, UUID id, ProjectTemplateRequest request) {
        var template = findTemplate(tenantId, id);
        template.setName(request.name());
        template.setDescription(request.description());
        template.setActive(request.active());
        return toTemplateResponse(tenantId, template);
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
        for (ProjectStageTemplate sourceStage : stageTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var stage = copyStageTemplate(sourceStage, copy);
            stage = stageTemplateRepository.save(stage);
            copyTemplateChecklist(tenantId, sourceStage, stage);
        }
        return toTemplateResponse(tenantId, copy);
    }

    @Transactional
    public void deleteTemplate(UUID tenantId, UUID id) {
        var template = findTemplate(tenantId, id);
        template.setDeleted(true);
        template.setDeletedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<StageTemplateResponse> listStageTemplates(UUID tenantId, UUID templateId) {
        findTemplate(tenantId, templateId);
        return stageTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(templateId, tenantId).stream()
                .map(stage -> toStageTemplateResponse(tenantId, stage))
                .toList();
    }

    @Transactional
    public StageTemplateResponse createStageTemplate(UUID tenantId, UUID templateId, StageTemplateRequest request) {
        var template = findTemplate(tenantId, templateId);
        var stage = new ProjectStageTemplate();
        stage.setTenant(template.getTenant());
        stage.setProjectTemplate(template);
        fillStageTemplate(stage, request);
        stage = stageTemplateRepository.save(stage);
        replaceStageTemplateChecklist(tenantId, stage, request.checklist());
        return toStageTemplateResponse(tenantId, stage);
    }

    @Transactional
    public StageTemplateResponse updateStageTemplate(UUID tenantId, UUID templateId, UUID stageId, StageTemplateRequest request) {
        findTemplate(tenantId, templateId);
        var stage = findStageTemplate(tenantId, stageId);
        if (!stage.getProjectTemplate().getId().equals(templateId)) {
            throw new NotFoundException("Etapa do modelo não encontrada.");
        }
        fillStageTemplate(stage, request);
        replaceStageTemplateChecklist(tenantId, stage, request.checklist());
        return toStageTemplateResponse(tenantId, stage);
    }

    @Transactional
    public StageTemplateResponse duplicateStageTemplate(UUID tenantId, UUID templateId, UUID stageId) {
        findTemplate(tenantId, templateId);
        var source = findStageTemplate(tenantId, stageId);
        var copy = copyStageTemplate(source, source.getProjectTemplate());
        copy.setName(source.getName() + " (cópia)");
        copy.setOrder(source.getOrder() + 1);
        copy = stageTemplateRepository.save(copy);
        copyTemplateChecklist(tenantId, source, copy);
        return toStageTemplateResponse(tenantId, copy);
    }

    @Transactional
    public void deleteStageTemplate(UUID tenantId, UUID templateId, UUID stageId) {
        findTemplate(tenantId, templateId);
        var stage = findStageTemplate(tenantId, stageId);
        stage.setDeleted(true);
        stage.setDeletedAt(Instant.now());
    }

    @Transactional
    public List<StageTemplateResponse> reorderStageTemplates(UUID tenantId, UUID templateId, ReorderRequest request) {
        findTemplate(tenantId, templateId);
        for (var item : request.items()) {
            var stage = findStageTemplate(tenantId, item.id());
            if (stage.getProjectTemplate().getId().equals(templateId)) {
                stage.setOrder(item.order());
            }
        }
        return listStageTemplates(tenantId, templateId);
    }

    @Transactional(readOnly = true)
    public List<ProjectStageResponse> listStages(UUID tenantId, UUID projectId) {
        findProject(tenantId, projectId);
        return stageRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(projectId, tenantId).stream()
                .map(stage -> toProjectStageResponse(tenantId, stage))
                .toList();
    }

    @Transactional
    public ProjectStageResponse createStage(UUID tenantId, UUID projectId, String username, ProjectStageRequest request) {
        var project = findProject(tenantId, projectId);
        var stage = new ProjectStage();
        stage.setTenant(project.getTenant());
        stage.setProject(project);
        fillProjectStage(tenantId, stage, request, username);
        stage = stageRepository.save(stage);
        replaceProjectChecklist(tenantId, stage, request.checklist());
        return toProjectStageResponse(tenantId, stage);
    }

    @Transactional
    public ProjectStageResponse updateStage(UUID tenantId, UUID projectId, UUID stageId, String username, ProjectStageRequest request) {
        findProject(tenantId, projectId);
        var stage = findStage(tenantId, stageId);
        if (!stage.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Etapa não encontrada.");
        }
        fillProjectStage(tenantId, stage, request, username);
        replaceProjectChecklist(tenantId, stage, request.checklist());
        return toProjectStageResponse(tenantId, stage);
    }

    @Transactional
    public ProjectStageResponse duplicateStage(UUID tenantId, UUID projectId, UUID stageId, String username) {
        findProject(tenantId, projectId);
        var source = findStage(tenantId, stageId);
        var copy = new ProjectStage();
        copy.setTenant(source.getTenant());
        copy.setProject(source.getProject());
        copy.setTemplate(source.getTemplate());
        copy.setName(source.getName() + " (cópia)");
        copy.setDescription(source.getDescription());
        copy.setOrder(source.getOrder() + 1);
        copy.setColor(source.getColor());
        copy.setIcon(source.getIcon());
        copy.setWeightPercentage(source.getWeightPercentage());
        copy.setCompletionPercentage(BigDecimal.ZERO);
        copy.setStatus(ProjectStageStatus.NOT_STARTED);
        copy.setResponsible(source.getResponsible());
        copy.setCreatedBy(username);
        copy.setUpdatedBy(username);
        copy = stageRepository.save(copy);
        for (ProjectStageChecklist item : stageChecklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(source.getId(), tenantId)) {
            var checklist = new ProjectStageChecklist();
            checklist.setTenant(copy.getTenant());
            checklist.setStage(copy);
            checklist.setTitle(item.getTitle());
            checklist.setOrder(item.getOrder());
            stageChecklistRepository.save(checklist);
        }
        return toProjectStageResponse(tenantId, copy);
    }

    @Transactional
    public void deleteStage(UUID tenantId, UUID projectId, UUID stageId) {
        findProject(tenantId, projectId);
        var stage = findStage(tenantId, stageId);
        stage.setDeleted(true);
        stage.setDeletedAt(Instant.now());
    }

    @Transactional
    public List<ProjectStageResponse> reorderStages(UUID tenantId, UUID projectId, ReorderRequest request) {
        findProject(tenantId, projectId);
        for (var item : request.items()) {
            var stage = findStage(tenantId, item.id());
            if (stage.getProject().getId().equals(projectId)) {
                stage.setOrder(item.order());
            }
        }
        return listStages(tenantId, projectId);
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
                predicates.add(builder.like(builder.lower(root.get("responsibleArchitect")), "%" + responsible.toLowerCase() + "%"));
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

    private ProjectStageTemplate findStageTemplate(UUID tenantId, UUID id) {
        return stageTemplateRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Etapa do modelo não encontrada."));
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
                displayName(project.getClient()), project.getProposal().getId(), project.getProposal().getNumber(),
                project.getTemplate() == null ? null : project.getTemplate().getId(),
                project.getTemplate() == null ? null : project.getTemplate().getName(),
                project.getResponsibleArchitect(), project.getStatus(), project.getExpectedEndDate(),
                project.getContractedValue(), projectProgress(project.getTenant().getId(), project.getId()), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectResponse toResponse(Project project) {
        var services = projectServiceRepository.findAllByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(service -> new ProjectServiceResponse(service.getId(), service.getName(), service.getDescription(),
                        service.getQuantity(), service.getUnit(), service.getContractedValue()))
                .toList();
        return new ProjectResponse(project.getId(), project.getCode(), project.getName(), project.getDescription(),
                project.getClient().getId(), displayName(project.getClient()), project.getProposal().getId(), project.getProposal().getNumber(),
                project.getTemplate() == null ? null : project.getTemplate().getId(),
                project.getTemplate() == null ? null : project.getTemplate().getName(),
                project.getResponsibleArchitect(), project.getStatus(), project.getStartDate(), project.getExpectedEndDate(),
                project.getCompletedAt(), project.getContractedValue(), projectProgress(project.getTenant().getId(), project.getId()),
                project.getInternalNotes(), project.getCreatedBy(), project.getUpdatedBy(), services,
                listStages(project.getTenant().getId(), project.getId()), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectTemplateResponse toTemplateResponse(UUID tenantId, ProjectTemplate template) {
        return new ProjectTemplateResponse(template.getId(), template.getName(), template.getDescription(),
                template.isActive(),
                stageTemplateRepository.countByProjectTemplateIdAndTenantIdAndDeletedFalse(template.getId(), tenantId),
                projectRepository.countByTenantIdAndTemplateIdAndDeletedFalse(tenantId, template.getId()),
                template.getCreatedAt(), template.getUpdatedAt());
    }

    private StageTemplateResponse toStageTemplateResponse(UUID tenantId, ProjectStageTemplate stage) {
        var checklist = stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId).stream()
                .map(item -> new StageChecklistResponse(item.getId(), item.getTitle(), item.getOrder(), false, null))
                .toList();
        return new StageTemplateResponse(stage.getId(), stage.getName(), stage.getDescription(), stage.getOrder(), stage.getColor(),
                stage.getIcon(), stage.getWeightPercentage(), stage.isActive(), checklist, stage.getCreatedAt(), stage.getUpdatedAt());
    }

    private ProjectStageResponse toProjectStageResponse(UUID tenantId, ProjectStage stage) {
        var checklist = stageChecklistRepository.findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(stage.getId(), tenantId).stream()
                .map(item -> new StageChecklistResponse(item.getId(), item.getTitle(), item.getOrder(), item.isCompleted(), item.getCompletedAt()))
                .toList();
        return new ProjectStageResponse(stage.getId(), stage.getTemplate() == null ? null : stage.getTemplate().getId(),
                stage.getDependsOnStage() == null ? null : stage.getDependsOnStage().getId(), stage.getName(), stage.getDescription(),
                stage.getOrder(), stage.getColor(), stage.getIcon(), stage.getStatus(), stage.getPlannedStart(), stage.getPlannedEnd(),
                stage.getActualStart(), stage.getActualEnd(), stage.getCompletionPercentage(), stage.getWeightPercentage(),
                stage.getResponsible(), stage.getNotes(), checklist, stage.getCreatedAt(), stage.getUpdatedAt());
    }

    private void fillStageTemplate(ProjectStageTemplate stage, StageTemplateRequest request) {
        stage.setName(request.name());
        stage.setDescription(request.description());
        stage.setOrder(request.order());
        stage.setColor(request.color());
        stage.setIcon(request.icon());
        stage.setWeightPercentage(scale(request.weightPercentage()));
        stage.setActive(request.active());
    }

    private void fillProjectStage(UUID tenantId, ProjectStage stage, ProjectStageRequest request, String username) {
        stage.setName(request.name());
        stage.setDescription(request.description());
        stage.setOrder(request.order());
        stage.setColor(request.color());
        stage.setIcon(request.icon());
        stage.setStatus(request.status() == null ? ProjectStageStatus.NOT_STARTED : request.status());
        stage.setPlannedStart(request.plannedStart());
        stage.setPlannedEnd(request.plannedEnd());
        stage.setActualStart(request.actualStart());
        stage.setActualEnd(request.actualEnd());
        stage.setCompletionPercentage(scale(request.completionPercentage()));
        stage.setWeightPercentage(scale(request.weightPercentage()));
        stage.setResponsible(request.responsible());
        stage.setNotes(request.notes());
        stage.setUpdatedBy(username);
        if (stage.getCreatedBy() == null) {
            stage.setCreatedBy(username);
        }
        stage.setDependsOnStage(request.dependsOnStageId() == null ? null : findStage(tenantId, request.dependsOnStageId()));
    }

    private ProjectStageTemplate copyStageTemplate(ProjectStageTemplate source, ProjectTemplate targetTemplate) {
        var stage = new ProjectStageTemplate();
        stage.setTenant(source.getTenant());
        stage.setProjectTemplate(targetTemplate);
        stage.setName(source.getName());
        stage.setDescription(source.getDescription());
        stage.setOrder(source.getOrder());
        stage.setColor(source.getColor());
        stage.setIcon(source.getIcon());
        stage.setWeightPercentage(source.getWeightPercentage());
        stage.setActive(source.isActive());
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
            var item = new ProjectStageChecklist();
            item.setTenant(stage.getTenant());
            item.setStage(stage);
            item.setTitle(request.title());
            item.setOrder(request.order());
            item.setCompleted(request.completed());
            item.setCompletedAt(request.completed() ? Instant.now() : null);
            stageChecklistRepository.save(item);
        }
    }

    private void copyStagesFromTemplate(UUID tenantId, Project project, ProjectTemplate template, String username) {
        for (ProjectStageTemplate templateStage : stageTemplateRepository.findAllByProjectTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(template.getId(), tenantId)) {
            var stage = new ProjectStage();
            stage.setTenant(project.getTenant());
            stage.setProject(project);
            stage.setTemplate(templateStage);
            stage.setName(templateStage.getName());
            stage.setDescription(templateStage.getDescription());
            stage.setOrder(templateStage.getOrder());
            stage.setColor(templateStage.getColor());
            stage.setIcon(templateStage.getIcon());
            stage.setWeightPercentage(templateStage.getWeightPercentage());
            stage.setCreatedBy(username);
            stage.setUpdatedBy(username);
            stage = stageRepository.save(stage);
            for (ProjectStageTemplateChecklist templateItem : stageTemplateChecklistRepository.findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(templateStage.getId(), tenantId)) {
                var checklist = new ProjectStageChecklist();
                checklist.setTenant(project.getTenant());
                checklist.setStage(stage);
                checklist.setTitle(templateItem.getTitle());
                checklist.setOrder(templateItem.getOrder());
                stageChecklistRepository.save(checklist);
            }
        }
    }

    private BigDecimal projectProgress(UUID tenantId, UUID projectId) {
        var stages = stageRepository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(projectId, tenantId);
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
}
