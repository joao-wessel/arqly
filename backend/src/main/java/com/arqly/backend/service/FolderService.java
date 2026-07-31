package com.arqly.backend.service;

import com.arqly.backend.dto.FileDtos.FolderRequest;
import com.arqly.backend.dto.FileDtos.FolderResponse;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.Folder;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.FileMapper;
import com.arqly.backend.repository.FileResourceRepository;
import com.arqly.backend.repository.FolderRepository;
import com.arqly.backend.repository.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {
    private final FolderRepository repository;
    private final FileResourceRepository fileRepository;
    private final FileOwnershipService ownershipService;
    private final FileMapper mapper;
    private final TenantRepository tenantRepository;

    public FolderService(FolderRepository repository, FileResourceRepository fileRepository,
                         FileOwnershipService ownershipService, FileMapper mapper, TenantRepository tenantRepository) {
        this.repository = repository;
        this.fileRepository = fileRepository;
        this.ownershipService = ownershipService;
        this.mapper = mapper;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> list(UUID tenantId, com.arqly.backend.entity.FileOwnerType ownerType, UUID ownerId) {
        ownershipService.validate(tenantId, ownerType, ownerId);
        return repository.findAllByTenantIdAndOwnerTypeAndOwnerIdAndDeletedFalseOrderByNameAsc(tenantId, ownerType, ownerId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public FolderResponse create(UUID tenantId, FolderRequest request) {
        ownershipService.validate(tenantId, request.ownerType(), request.ownerId());
        var parent = request.parentId() == null ? null : find(tenantId, request.parentId());
        validateParent(request, parent);
        ensureUnique(tenantId, request, parent);
        var folder = new Folder();
        folder.setTenant(tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Escritório não encontrado.")));
        folder.setOwnerType(request.ownerType());
        folder.setOwnerId(request.ownerId());
        folder.setParent(parent);
        folder.setName(request.name().trim());
        return mapper.toResponse(repository.save(folder));
    }

    @Transactional
    public FolderResponse rename(UUID tenantId, UUID id, String name) {
        if (name == null || name.isBlank()) throw new BusinessException("Informe o nome da pasta.");
        var folder = find(tenantId, id);
        if (folder.getName().equalsIgnoreCase(name.trim())) {
            return mapper.toResponse(folder);
        }
        var request = new FolderRequest(folder.getOwnerType(), folder.getOwnerId(),
                folder.getParent() == null ? null : folder.getParent().getId(), name.trim());
        ensureUnique(tenantId, request, folder.getParent());
        folder.setName(name.trim());
        return mapper.toResponse(folder);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var folder = find(tenantId, id);
        if (repository.existsByParentIdAndDeletedFalse(id)
                || fileRepository.existsByFolderIdAndStatusNot(id, FileResourceStatus.DELETED)) {
            throw new BusinessException("A pasta precisa estar vazia para ser excluída.");
        }
        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
    }

    public Folder find(UUID tenantId, UUID id) {
        return repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));
    }

    private void validateParent(FolderRequest request, Folder parent) {
        if (parent != null && (parent.getOwnerType() != request.ownerType() || !parent.getOwnerId().equals(request.ownerId()))) {
            throw new BusinessException("A pasta pai pertence a outro contexto.");
        }
    }

    private void ensureUnique(UUID tenantId, FolderRequest request, Folder parent) {
        var parentId = parent == null ? null : parent.getId();
        if (repository.existsByTenantIdAndOwnerTypeAndOwnerIdAndParentIdAndNameIgnoreCaseAndDeletedFalse(
                tenantId, request.ownerType(), request.ownerId(), parentId, request.name().trim())) {
            throw new BusinessException("Já existe uma pasta com esse nome neste local.");
        }
    }
}
