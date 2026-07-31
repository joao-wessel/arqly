package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.DocumentDtos.DocumentPreviewRequest;
import com.arqly.backend.dto.DocumentDtos.DocumentPreviewResponse;
import com.arqly.backend.dto.DocumentDtos.GenerateDocumentRequest;
import com.arqly.backend.dto.DocumentDtos.GeneratedDocumentResponse;
import com.arqly.backend.dto.DocumentDtos.GeneratedDocumentSummaryResponse;
import com.arqly.backend.dto.DocumentDtos.NewDocumentVersionRequest;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.DocumentCategory;
import com.arqly.backend.entity.GeneratedDocument;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.GeneratedDocumentMapper;
import com.arqly.backend.repository.GeneratedDocumentRepository;
import com.arqly.backend.repository.TenantUserRepository;
import com.arqly.backend.service.document.DocumentContext;
import com.arqly.backend.service.document.DocumentContextFactory;
import com.arqly.backend.service.document.TemplateRenderer;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentGenerationService {
    private final GeneratedDocumentRepository repository;
    private final DocumentTemplateService templateService;
    private final DocumentContextFactory contextFactory;
    private final TemplateRenderer renderer;
    private final GeneratedDocumentMapper mapper;
    private final DocumentVersionService versionService;
    private final TenantUserRepository userRepository;
    private final ActivityEventPublisher activityPublisher;

    public DocumentGenerationService(GeneratedDocumentRepository repository, DocumentTemplateService templateService,
                                     DocumentContextFactory contextFactory, List<TemplateRenderer> renderers,
                                     GeneratedDocumentMapper mapper, DocumentVersionService versionService,
                                     TenantUserRepository userRepository, ActivityEventPublisher activityPublisher) {
        this.repository = repository;
        this.templateService = templateService;
        this.contextFactory = contextFactory;
        this.renderer = renderers.stream().filter(item -> "MARKDOWN".equals(item.format())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Renderer Markdown não configurado."));
        this.mapper = mapper;
        this.versionService = versionService;
        this.userRepository = userRepository;
        this.activityPublisher = activityPublisher;
    }

    @Transactional(readOnly = true)
    public Page<GeneratedDocumentSummaryResponse> list(UUID tenantId, String search, DocumentCategory category,
                                                        GeneratedDocumentStatus status, UUID projectId, UUID proposalId,
                                                        UUID clientId, Pageable pageable) {
        return repository.findAll(specification(tenantId, search, category, status, projectId, proposalId, clientId), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public GeneratedDocumentResponse get(UUID tenantId, UUID id) { return toResponse(find(tenantId, id)); }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentResponse> versions(UUID tenantId, UUID id) {
        var document = find(tenantId, id);
        return repository.findAllByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(tenantId, document.getSeriesId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse preview(UUID tenantId, DocumentPreviewRequest request) {
        var template = templateService.find(tenantId, request.templateId());
        var context = contextFactory.create(tenantId, request.projectId(), request.proposalId(), request.clientId());
        var result = renderer.render(template.getContent(), context);
        return new DocumentPreviewResponse(template.getId(),
                request.title() == null || request.title().isBlank() ? template.getName() : request.title(),
                result.content(), result.unresolvedVariables(),
                id(context.project()), context.project() == null ? null : context.project().getName(),
                id(context.proposal()), context.proposal() == null ? null : context.proposal().getNumber(),
                context.client().getId(), displayName(context.client()));
    }

    @Transactional
    public GeneratedDocumentResponse generate(UUID tenantId, UUID userId, String actor, GenerateDocumentRequest request) {
        var template = templateService.find(tenantId, request.templateId());
        var context = contextFactory.create(tenantId, request.projectId(), request.proposalId(), request.clientId());
        var document = mapper.toEntity(request);
        document.setTenant(context.tenant());
        document.setTemplate(template);
        document.setProject(context.project());
        document.setProposal(context.proposal());
        document.setClient(context.client());
        document.setSeriesId(UUID.randomUUID());
        document.setVersion(1);
        document.setStatus(request.status() == null ? GeneratedDocumentStatus.GENERATED : request.status());
        applyAuthor(document, tenantId, userId, actor);
        document = repository.save(document);
        publish(document, userId, actor, "Documento gerado", "gerou " + document.getTitle());
        return toResponse(document);
    }

    @Transactional
    public GeneratedDocumentResponse newVersion(UUID tenantId, UUID id, UUID userId, String actor, NewDocumentVersionRequest request) {
        var selected = find(tenantId, id);
        var latest = repository.findFirstByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(tenantId, selected.getSeriesId())
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
        var version = versionService.newDocumentVersion(latest, request);
        applyAuthor(version, tenantId, userId, actor);
        version = repository.save(version);
        publish(version, userId, actor, "Documento atualizado", "criou a versão " + version.getVersion() + " de " + version.getTitle());
        return toResponse(version);
    }

    @Transactional
    public GeneratedDocumentResponse duplicate(UUID tenantId, UUID id, UUID userId, String actor) {
        var copy = versionService.duplicateDocument(find(tenantId, id));
        applyAuthor(copy, tenantId, userId, actor);
        copy = repository.save(copy);
        publish(copy, userId, actor, "Documento duplicado", "duplicou " + copy.getTitle());
        return toResponse(copy);
    }

    @Transactional
    public GeneratedDocumentResponse archive(UUID tenantId, UUID id, UUID userId, String actor) {
        var document = find(tenantId, id);
        document.setStatus(GeneratedDocumentStatus.ARCHIVED);
        document.setClientVisible(false);
        publish(document, userId, actor, "Documento arquivado", "arquivou " + document.getTitle());
        return toResponse(document);
    }

    @Transactional
    public GeneratedDocumentResponse publishToPortal(UUID tenantId, UUID id) {
        var document = find(tenantId, id);
        if (document.getStatus() == GeneratedDocumentStatus.ARCHIVED) {
            throw new com.arqly.backend.exception.BusinessException("Documentos arquivados nÃ£o podem ser publicados no portal.");
        }
        document.setClientVisible(true);
        return toResponse(document);
    }

    @Transactional
    public GeneratedDocumentResponse hideFromPortal(UUID tenantId, UUID id) {
        var document = find(tenantId, id);
        document.setClientVisible(false);
        return toResponse(document);
    }

    public GeneratedDocument find(UUID tenantId, UUID id) {
        return repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
    }

    private void applyAuthor(GeneratedDocument document, UUID tenantId, UUID userId, String actor) {
        var user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        document.setGeneratedBy(user);
        document.setGeneratedByName(user.getName());
        document.setGeneratedAt(Instant.now());
    }

    private void publish(GeneratedDocument document, UUID userId, String actor, String title, String description) {
        activityPublisher.publishDocument(document.getTenant().getId(), id(document.getProject()),
                id(document.getProposal()), document.getClient().getId(), document.getId(), userId, actor,
                ActivityType.DOCUMENT_GENERATED, title, description,
                "{\"documentId\":\"" + document.getId() + "\",\"version\":" + document.getVersion() + "}");
    }

    private Specification<GeneratedDocument> specification(UUID tenantId, String search, DocumentCategory category,
                                                             GeneratedDocumentStatus status, UUID projectId,
                                                             UUID proposalId, UUID clientId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            predicates.add(builder.isTrue(root.get("currentVersion")));
            if (search != null && !search.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (category != null) predicates.add(builder.equal(root.get("template").get("category"), category));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (projectId != null) predicates.add(builder.equal(root.get("project").get("id"), projectId));
            if (proposalId != null) predicates.add(builder.equal(root.get("proposal").get("id"), proposalId));
            if (clientId != null) predicates.add(builder.equal(root.get("client").get("id"), clientId));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private GeneratedDocumentSummaryResponse toSummary(GeneratedDocument document) {
        return new GeneratedDocumentSummaryResponse(document.getId(), document.getSeriesId(), document.getTemplate().getId(),
                document.getTemplate().getName(), document.getTemplate().getCategory(), id(document.getProject()),
                document.getProject() == null ? null : document.getProject().getName(), id(document.getProposal()),
                document.getProposal() == null ? null : document.getProposal().getNumber(), document.getClient().getId(),
                displayName(document.getClient()), document.getTitle(), document.getVersion(), document.getStatus(),
                id(document.getGeneratedBy()), document.getGeneratedByName(), document.isClientVisible(), document.getGeneratedAt());
    }

    private GeneratedDocumentResponse toResponse(GeneratedDocument document) {
        return new GeneratedDocumentResponse(document.getId(), document.getSeriesId(), id(document.getPreviousVersion()),
                document.getTemplate().getId(), document.getTemplate().getName(), document.getTemplate().getCategory(),
                id(document.getProject()), document.getProject() == null ? null : document.getProject().getName(),
                id(document.getProposal()), document.getProposal() == null ? null : document.getProposal().getNumber(),
                document.getClient().getId(), displayName(document.getClient()), document.getTitle(), document.getContent(),
                document.getVersion(), document.getStatus(), id(document.getGeneratedBy()), document.getGeneratedByName(),
                document.isClientVisible(), document.getGeneratedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }

    private UUID id(com.arqly.backend.entity.BaseEntity entity) { return entity == null ? null : entity.getId(); }
    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }
}
