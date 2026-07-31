package com.arqly.backend.service;

import com.arqly.backend.dto.FileDtos.FileResponse;
import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResource;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.mapper.FileMapper;
import com.arqly.backend.repository.FileResourceRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileSearchService {
    private static final Set<String> PREVIEW_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp", "svg", "txt", "md", "markdown");
    private final FileResourceRepository repository;
    private final FileMapper mapper;
    private final FileOwnershipService ownershipService;

    public FileSearchService(FileResourceRepository repository, FileMapper mapper, FileOwnershipService ownershipService) {
        this.repository = repository;
        this.mapper = mapper;
        this.ownershipService = ownershipService;
    }

    @Transactional(readOnly = true)
    public Page<FileResponse> search(UUID tenantId, FileOwnerType ownerType, UUID ownerId, UUID folderId,
                                     Boolean rootOnly, String search, String extension, String author,
                                     String tag, FileResourceStatus status, Pageable pageable) {
        if (ownerId != null && ownerType == null) {
            throw new com.arqly.backend.exception.BusinessException("Informe o tipo do proprietário do arquivo.");
        }
        if (ownerType != null && ownerId != null) ownershipService.validate(tenantId, ownerType, ownerId);
        return repository.findAll(specification(tenantId, ownerType, ownerId, folderId, rootOnly, search,
                extension, author, tag, status), pageable).map(this::response);
    }

    public boolean previewAvailable(FileResource file) {
        return PREVIEW_EXTENSIONS.contains(file.getExtension().toLowerCase());
    }

    public FileResponse response(FileResource file) {
        var value = mapper.toResponse(file);
        var ownerLabel = ownershipService.validate(file.getTenant().getId(), file.getOwnerType(), file.getOwnerId()).label();
        return new FileResponse(value.id(), value.ownerType(), value.ownerId(), ownerLabel, value.folderId(), value.folderName(),
                value.name(), value.originalName(), value.extension(), value.mimeType(), value.size(), value.checksum(),
                value.version(), value.visibility(), value.status(), value.uploadedById(), value.uploadedByName(),
                value.tags(), previewAvailable(file), value.createdAt(), value.updatedAt());
    }

    private Specification<FileResource> specification(UUID tenantId, FileOwnerType ownerType, UUID ownerId,
                                                       UUID folderId, Boolean rootOnly, String search,
                                                       String extension, String author, String tag,
                                                       FileResourceStatus status) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.equal(root.get("status"), status == null ? FileResourceStatus.ACTIVE : status));
            if (ownerType != null) predicates.add(builder.equal(root.get("ownerType"), ownerType));
            if (ownerId != null) predicates.add(builder.equal(root.get("ownerId"), ownerId));
            if (folderId != null) predicates.add(builder.equal(root.get("folder").get("id"), folderId));
            else if (Boolean.TRUE.equals(rootOnly)) predicates.add(builder.isNull(root.get("folder")));
            if (search != null && !search.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (extension != null && !extension.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("extension")), extension.trim().toLowerCase()));
            }
            if (author != null && !author.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("uploadedBy").get("name")),
                        "%" + author.trim().toLowerCase() + "%"));
            }
            if (tag != null && !tag.isBlank()) {
                var tags = root.join("tags", JoinType.LEFT);
                predicates.add(builder.equal(builder.lower(tags.get("name")), tag.trim().toLowerCase()));
                query.distinct(true);
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
