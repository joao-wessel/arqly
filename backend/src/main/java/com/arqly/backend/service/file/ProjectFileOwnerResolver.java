package com.arqly.backend.service.file;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProjectFileOwnerResolver implements FileOwnerResolver {
    private final ProjectRepository repository;
    public ProjectFileOwnerResolver(ProjectRepository repository) { this.repository = repository; }
    public FileOwnerType type() { return FileOwnerType.PROJECT; }
    public FileOwnerContext resolve(UUID tenantId, UUID ownerId) {
        var value = repository.findByIdAndTenantIdAndDeletedFalse(ownerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
        return new FileOwnerContext(ownerId, value.getName(), value.getId(), null, null,
                value.getProposal() == null ? null : value.getProposal().getId(), value.getClient().getId());
    }
}
