package com.arqly.backend.service;

import com.arqly.backend.dto.FileDtos.FileVersionResponse;
import com.arqly.backend.entity.FileResource;
import com.arqly.backend.entity.FileVersion;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.mapper.FileMapper;
import com.arqly.backend.repository.FileVersionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileVersionService {
    private final FileVersionRepository repository;
    private final FileMapper mapper;

    public FileVersionService(FileVersionRepository repository, FileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public FileVersion register(FileResource file, TenantUser author, String storageKey, String checksum,
                                long size, int versionNumber, String comment) {
        var version = new FileVersion();
        version.setFile(file);
        version.setAuthor(author);
        version.setStorageKey(storageKey);
        version.setChecksum(checksum);
        version.setSize(size);
        version.setVersionNumber(versionNumber);
        version.setRevisionComment(comment == null || comment.isBlank() ? null : comment.trim());
        return repository.save(version);
    }

    @Transactional(readOnly = true)
    public List<FileVersionResponse> list(UUID tenantId, FileResource file) {
        if (!file.getTenant().getId().equals(tenantId)) return List.of();
        return repository.findAllByFileIdOrderByVersionNumberDesc(file.getId()).stream().map(mapper::toResponse).toList();
    }

    public FileVersion find(UUID tenantId, UUID versionId) {
        return repository.findByIdAndFileTenantId(versionId, tenantId)
                .orElseThrow(() -> new com.arqly.backend.exception.NotFoundException("Versão do arquivo não encontrada."));
    }
}
