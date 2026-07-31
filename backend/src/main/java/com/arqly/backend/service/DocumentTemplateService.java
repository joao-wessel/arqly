package com.arqly.backend.service;

import com.arqly.backend.dto.DocumentDtos.DocumentTemplateRequest;
import com.arqly.backend.dto.DocumentDtos.DocumentTemplateResponse;
import com.arqly.backend.dto.DocumentDtos.DocumentTemplateSummaryResponse;
import com.arqly.backend.entity.DocumentCategory;
import com.arqly.backend.entity.DocumentTemplate;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.DocumentTemplateMapper;
import com.arqly.backend.repository.DocumentTemplateRepository;
import com.arqly.backend.repository.TenantRepository;
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
public class DocumentTemplateService {
    private final DocumentTemplateRepository repository;
    private final TenantRepository tenantRepository;
    private final DocumentTemplateMapper mapper;
    private final DocumentVersionService versionService;

    public DocumentTemplateService(DocumentTemplateRepository repository, TenantRepository tenantRepository,
                                   DocumentTemplateMapper mapper, DocumentVersionService versionService) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.mapper = mapper;
        this.versionService = versionService;
    }

    @Transactional(readOnly = true)
    public Page<DocumentTemplateSummaryResponse> list(UUID tenantId, String search, DocumentCategory category,
                                                       Boolean active, Pageable pageable) {
        return repository.findAll(specification(tenantId, search, category, active), pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateSummaryResponse> options(UUID tenantId) {
        return repository.findAllByTenantIdAndActiveTrueAndArchivedFalseAndDeletedFalseOrderByNameAsc(tenantId).stream()
                .filter(DocumentTemplate::isCurrentVersion)
                .map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public DocumentTemplateResponse get(UUID tenantId, UUID id) { return toResponse(find(tenantId, id)); }

    @Transactional(readOnly = true)
    public List<DocumentTemplateResponse> versions(UUID tenantId, UUID id) {
        var template = find(tenantId, id);
        return repository.findAllByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(tenantId, template.getSeriesId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public DocumentTemplateResponse create(UUID tenantId, DocumentTemplateRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var template = mapper.toEntity(request);
        template.setTenant(tenant);
        template.setSeriesId(UUID.randomUUID());
        return toResponse(repository.save(template));
    }

    @Transactional
    public DocumentTemplateResponse update(UUID tenantId, UUID id, DocumentTemplateRequest request) {
        var template = find(tenantId, id);
        mapper.update(request, template);
        return toResponse(template);
    }

    @Transactional
    public DocumentTemplateResponse newVersion(UUID tenantId, UUID id, DocumentTemplateRequest request) {
        var latest = latest(tenantId, find(tenantId, id).getSeriesId());
        return toResponse(repository.save(versionService.newTemplateVersion(latest, request)));
    }

    @Transactional
    public DocumentTemplateResponse duplicate(UUID tenantId, UUID id) {
        return toResponse(repository.save(versionService.duplicateTemplate(find(tenantId, id))));
    }

    @Transactional
    public DocumentTemplateResponse setActive(UUID tenantId, UUID id, boolean active) {
        var template = find(tenantId, id);
        template.setActive(active);
        return toResponse(template);
    }

    @Transactional
    public DocumentTemplateResponse archive(UUID tenantId, UUID id) {
        var template = find(tenantId, id);
        template.setArchived(true);
        template.setActive(false);
        return toResponse(template);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var template = find(tenantId, id);
        template.setDeleted(true);
        template.setDeletedAt(Instant.now());
        template.setActive(false);
    }

    public DocumentTemplate find(UUID tenantId, UUID id) {
        return repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Modelo de documento não encontrado."));
    }

    private DocumentTemplate latest(UUID tenantId, UUID seriesId) {
        return repository.findFirstByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(tenantId, seriesId)
                .orElseThrow(() -> new NotFoundException("Modelo de documento não encontrado."));
    }

    private Specification<DocumentTemplate> specification(UUID tenantId, String search, DocumentCategory category, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            predicates.add(builder.isTrue(root.get("currentVersion")));
            if (search != null && !search.isBlank()) {
                var term = "%" + search.toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), term),
                        builder.like(builder.lower(root.get("description")), term)));
            }
            if (category != null) predicates.add(builder.equal(root.get("category"), category));
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private DocumentTemplateSummaryResponse toSummary(DocumentTemplate template) {
        return new DocumentTemplateSummaryResponse(template.getId(), template.getSeriesId(), template.getName(),
                template.getDescription(), template.getCategory(), template.isActive(), template.isArchived(),
                template.getVersion(), template.getCreatedAt(), template.getUpdatedAt());
    }

    private DocumentTemplateResponse toResponse(DocumentTemplate template) {
        return new DocumentTemplateResponse(template.getId(), template.getSeriesId(),
                template.getPreviousVersion() == null ? null : template.getPreviousVersion().getId(),
                template.getName(), template.getDescription(), template.getCategory(), template.getContent(),
                template.isActive(), template.isArchived(), template.getVersion(), template.getCreatedAt(), template.getUpdatedAt());
    }
}
