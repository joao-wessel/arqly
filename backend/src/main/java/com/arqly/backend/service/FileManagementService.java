package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.FileDtos.FileMoveRequest;
import com.arqly.backend.dto.FileDtos.FileResponse;
import com.arqly.backend.dto.FileDtos.FileTagResponse;
import com.arqly.backend.dto.FileDtos.FileUpdateRequest;
import com.arqly.backend.dto.FileDtos.FileUploadMetadata;
import com.arqly.backend.dto.FileDtos.FileVersionResponse;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.FileResource;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.FileTag;
import com.arqly.backend.entity.FileVisibility;
import com.arqly.backend.entity.Folder;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.FileResourceRepository;
import com.arqly.backend.repository.FileTagRepository;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import com.arqly.backend.service.file.FileOwnerContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileManagementService {
    private final FileResourceRepository repository;
    private final FileTagRepository tagRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository userRepository;
    private final FolderService folderService;
    private final FileStorageService storageService;
    private final FileVersionService versionService;
    private final FileSearchService searchService;
    private final FileOwnershipService ownershipService;
    private final ActivityEventPublisher activityPublisher;

    public FileManagementService(FileResourceRepository repository, FileTagRepository tagRepository,
                                 TenantRepository tenantRepository, TenantUserRepository userRepository,
                                 FolderService folderService, FileStorageService storageService,
                                 FileVersionService versionService, FileSearchService searchService,
                                 FileOwnershipService ownershipService, ActivityEventPublisher activityPublisher) {
        this.repository = repository;
        this.tagRepository = tagRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.folderService = folderService;
        this.storageService = storageService;
        this.versionService = versionService;
        this.searchService = searchService;
        this.ownershipService = ownershipService;
        this.activityPublisher = activityPublisher;
    }

    @Transactional
    public FileResponse upload(UUID tenantId, UUID userId, String actor, MultipartFile upload,
                               FileUploadMetadata metadata) {
        if (upload == null || upload.isEmpty()) throw new BusinessException("Selecione um arquivo para enviar.");
        var owner = ownershipService.validate(tenantId, metadata.ownerType(), metadata.ownerId());
        var folder = metadata.folderId() == null ? null : folderService.find(tenantId, metadata.folderId());
        validateFolder(folder, metadata.ownerType(), metadata.ownerId());
        var originalName = cleanName(upload.getOriginalFilename());
        var existing = repository.findFirstByTenantIdAndOwnerTypeAndOwnerIdAndFolderIdAndNameIgnoreCaseAndStatusNot(
                tenantId, metadata.ownerType(), metadata.ownerId(), metadata.folderId(), originalName, FileResourceStatus.DELETED);
        if (existing.isPresent()) {
            if ("NEW_VERSION".equalsIgnoreCase(metadata.conflictStrategy())) {
                return newVersion(tenantId, userId, actor, existing.get().getId(), upload, metadata.revisionComment());
            }
            if ("RENAME".equalsIgnoreCase(metadata.conflictStrategy())) {
                originalName = availableName(tenantId, metadata, originalName);
            } else {
                throw new BusinessException("Já existe um arquivo com esse nome. Escolha nova versão ou renomear.");
            }
        }
        var extension = extension(originalName);
        var stored = store(tenantId, extension, upload);
        var user = user(tenantId, userId);
        var file = new FileResource();
        file.setTenant(tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Escritório não encontrado.")));
        file.setOwnerType(metadata.ownerType());
        file.setOwnerId(metadata.ownerId());
        file.setFolder(folder);
        file.setName(originalName);
        file.setOriginalName(cleanName(upload.getOriginalFilename()));
        file.setExtension(extension);
        file.setMimeType(contentType(upload, extension));
        file.setSize(stored.size());
        file.setStorageKey(stored.storageKey());
        file.setChecksum(stored.checksum());
        file.setVersion(1);
        file.setVisibility(metadata.visibility() == null ? FileVisibility.INTERNAL : metadata.visibility());
        file.setStatus(FileResourceStatus.ACTIVE);
        file.setUploadedBy(user);
        file.setTags(tags(tenantId, metadata.tags()));
        file = repository.save(file);
        versionService.register(file, user, stored.storageKey(), stored.checksum(), stored.size(), 1, metadata.revisionComment());
        publish(file, owner, userId, actor, ActivityType.FILE_UPLOADED, "Arquivo enviado",
                "enviou " + file.getName());
        return searchService.response(file);
    }

    @Transactional
    public FileResponse newVersion(UUID tenantId, UUID userId, String actor, UUID fileId,
                                   MultipartFile upload, String comment) {
        if (upload == null || upload.isEmpty()) throw new BusinessException("Selecione o arquivo da nova versão.");
        var file = find(tenantId, fileId);
        if (file.getStatus() == FileResourceStatus.DELETED) throw new BusinessException("Restaure o arquivo antes de versioná-lo.");
        var owner = ownershipService.validate(tenantId, file.getOwnerType(), file.getOwnerId());
        var extension = extension(cleanName(upload.getOriginalFilename()));
        var stored = store(tenantId, extension, upload);
        var user = user(tenantId, userId);
        var nextVersion = file.getVersion() + 1;
        versionService.register(file, user, stored.storageKey(), stored.checksum(), stored.size(), nextVersion, comment);
        file.setOriginalName(cleanName(upload.getOriginalFilename()));
        file.setExtension(extension);
        file.setMimeType(contentType(upload, extension));
        file.setSize(stored.size());
        file.setStorageKey(stored.storageKey());
        file.setChecksum(stored.checksum());
        file.setVersion(nextVersion);
        file.setUploadedBy(user);
        file.setStatus(FileResourceStatus.ACTIVE);
        publish(file, owner, userId, actor, ActivityType.FILE_VERSIONED, "Nova versão de arquivo",
                "enviou a versão " + nextVersion + " de " + file.getName());
        return searchService.response(file);
    }

    @Transactional(readOnly = true)
    public FileResponse get(UUID tenantId, UUID id) {
        var file = find(tenantId, id);
        ownershipService.validate(tenantId, file.getOwnerType(), file.getOwnerId());
        return searchService.response(file);
    }

    @Transactional(readOnly = true)
    public List<FileVersionResponse> versions(UUID tenantId, UUID id) {
        return versionService.list(tenantId, find(tenantId, id));
    }

    @Transactional(readOnly = true)
    public List<FileTagResponse> tags(UUID tenantId) {
        return tagRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(tag -> new FileTagResponse(tag.getId(), tag.getName())).toList();
    }

    @Transactional
    public FileResponse update(UUID tenantId, UUID userId, String actor, UUID id, FileUpdateRequest request) {
        var file = find(tenantId, id);
        var owner = ownershipService.validate(tenantId, file.getOwnerType(), file.getOwnerId());
        if (request.name() != null && !request.name().isBlank() && !request.name().trim().equals(file.getName())) {
            var previous = file.getName();
            ensureNameAvailable(tenantId, file, request.name().trim());
            file.setName(request.name().trim());
            publish(file, owner, userId, actor, ActivityType.FILE_RENAMED, "Arquivo renomeado",
                    "renomeou " + previous + " para " + file.getName());
        }
        if (request.folderId() != null || file.getFolder() != null) {
            var folder = request.folderId() == null ? null : folderService.find(tenantId, request.folderId());
            validateFolder(folder, file.getOwnerType(), file.getOwnerId());
            if (!java.util.Objects.equals(request.folderId(), file.getFolder() == null ? null : file.getFolder().getId())) {
                file.setFolder(folder);
                publish(file, owner, userId, actor, ActivityType.FILE_MOVED, "Arquivo movido",
                        "moveu " + file.getName() + " para " + (folder == null ? "Raiz" : folder.getName()));
            }
        }
        if (request.visibility() != null) file.setVisibility(request.visibility());
        if (request.tags() != null) file.setTags(tags(tenantId, request.tags()));
        return searchService.response(file);
    }

    @Transactional
    public FileResponse move(UUID tenantId, UUID userId, String actor, UUID id, FileMoveRequest request) {
        var file = find(tenantId, id);
        var destination = ownershipService.validate(tenantId, request.ownerType(), request.ownerId());
        var folder = request.folderId() == null ? null : folderService.find(tenantId, request.folderId());
        validateFolder(folder, request.ownerType(), request.ownerId());
        file.setOwnerType(request.ownerType());
        file.setOwnerId(request.ownerId());
        file.setFolder(folder);
        publish(file, destination, userId, actor, ActivityType.FILE_MOVED, "Arquivo movido",
                "moveu " + file.getName() + " para " + destination.label());
        return searchService.response(file);
    }

    @Transactional
    public FileResponse changeStatus(UUID tenantId, UUID userId, String actor, UUID id, FileResourceStatus status) {
        var file = find(tenantId, id);
        var owner = ownershipService.validate(tenantId, file.getOwnerType(), file.getOwnerId());
        file.setStatus(status);
        file.setDeletedAt(status == FileResourceStatus.DELETED ? Instant.now() : null);
        var type = status == FileResourceStatus.ARCHIVED ? ActivityType.FILE_ARCHIVED : ActivityType.FILE_RESTORED;
        var title = status == FileResourceStatus.ARCHIVED ? "Arquivo arquivado" : "Arquivo restaurado";
        publish(file, owner, userId, actor, type, title,
                (status == FileResourceStatus.ARCHIVED ? "arquivou " : "restaurou ") + file.getName());
        return searchService.response(file);
    }

    public FileResource find(UUID tenantId, UUID id) {
        return repository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
    }

    public InputStream download(UUID tenantId, UUID userId, String actor, UUID id, UUID versionId) {
        var file = find(tenantId, id);
        var owner = ownershipService.validate(tenantId, file.getOwnerType(), file.getOwnerId());
        var key = file.getStorageKey();
        if (versionId != null) {
            var version = versionService.find(tenantId, versionId);
            if (!version.getFile().getId().equals(file.getId())) {
                throw new NotFoundException("Versão do arquivo não encontrada.");
            }
            key = version.getStorageKey();
        }
        publish(file, owner, userId, actor, ActivityType.FILE_DOWNLOADED, "Arquivo baixado",
                "baixou " + file.getName());
        return storageService.load(key);
    }

    private com.arqly.backend.service.file.StoredFile store(UUID tenantId, String extension, MultipartFile upload) {
        try {
            return storageService.store(tenantId, extension, upload.getInputStream());
        } catch (IOException exception) {
            throw new BusinessException("Não foi possível ler o arquivo enviado.");
        }
    }

    private com.arqly.backend.entity.TenantUser user(UUID tenantId, UUID userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private LinkedHashSet<FileTag> tags(UUID tenantId, List<String> values) {
        var result = new LinkedHashSet<FileTag>();
        if (values == null) return result;
        for (var value : values.stream().filter(item -> item != null && !item.isBlank()).map(String::trim).distinct().toList()) {
            result.add(tagRepository.findByTenantIdAndNameIgnoreCase(tenantId, value).orElseGet(() -> {
                var tag = new FileTag();
                tag.setTenant(tenantRepository.findById(tenantId).orElseThrow());
                tag.setName(value);
                return tagRepository.save(tag);
            }));
        }
        return result;
    }

    private void validateFolder(Folder folder, com.arqly.backend.entity.FileOwnerType ownerType, UUID ownerId) {
        if (folder != null && (folder.getOwnerType() != ownerType || !folder.getOwnerId().equals(ownerId))) {
            throw new BusinessException("A pasta pertence a outro contexto.");
        }
    }

    private void ensureNameAvailable(UUID tenantId, FileResource file, String name) {
        var conflict = repository.findFirstByTenantIdAndOwnerTypeAndOwnerIdAndFolderIdAndNameIgnoreCaseAndStatusNot(
                tenantId, file.getOwnerType(), file.getOwnerId(), file.getFolder() == null ? null : file.getFolder().getId(),
                name, FileResourceStatus.DELETED);
        if (conflict.isPresent() && !conflict.get().getId().equals(file.getId())) {
            throw new BusinessException("Já existe um arquivo com esse nome neste local.");
        }
    }

    private String availableName(UUID tenantId, FileUploadMetadata metadata, String name) {
        var extension = extension(name);
        var base = extension.isBlank() ? name : name.substring(0, name.length() - extension.length() - 1);
        for (int index = 2; index < 10_000; index++) {
            var candidate = base + " (" + index + ")" + (extension.isBlank() ? "" : "." + extension);
            if (repository.findFirstByTenantIdAndOwnerTypeAndOwnerIdAndFolderIdAndNameIgnoreCaseAndStatusNot(
                    tenantId, metadata.ownerType(), metadata.ownerId(), metadata.folderId(), candidate,
                    FileResourceStatus.DELETED).isEmpty()) return candidate;
        }
        throw new BusinessException("Não foi possível gerar um nome disponível.");
    }

    private String cleanName(String value) {
        var name = value == null ? "arquivo" : Path.of(value).getFileName().toString().trim();
        return name.isBlank() ? "arquivo" : name.replaceAll("[\\r\\n]", "_");
    }

    private String extension(String name) {
        var index = name.lastIndexOf('.');
        return index < 0 || index == name.length() - 1 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String contentType(MultipartFile upload, String extension) {
        if (upload.getContentType() != null && !upload.getContentType().isBlank()) return upload.getContentType();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "txt", "md", "markdown" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    private void publish(FileResource file, FileOwnerContext owner, UUID userId, String actor,
                         ActivityType type, String title, String description) {
        var metadata = "{\"fileId\":\"" + file.getId() + "\",\"version\":" + file.getVersion()
                + ",\"name\":\"" + file.getName().replace("\"", "\\\"") + "\"}";
        activityPublisher.publishFile(file.getTenant().getId(), owner.projectId(), owner.phaseId(), owner.stageId(),
                owner.proposalId(), owner.clientId(), userId, actor, type, title, description, metadata);
    }
}
