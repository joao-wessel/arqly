package com.arqly.backend.service.file;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ProjectStageRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProjectStageFileOwnerResolver implements FileOwnerResolver {
    private final ProjectStageRepository repository;
    public ProjectStageFileOwnerResolver(ProjectStageRepository repository) { this.repository = repository; }
    public FileOwnerType type() { return FileOwnerType.PROJECT_STAGE; }
    public FileOwnerContext resolve(UUID tenantId, UUID ownerId) {
        var value = repository.findByIdAndTenantIdAndDeletedFalse(ownerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Etapa não encontrada."));
        var phase = value.getProjectPhase();
        var project = phase.getProject();
        return new FileOwnerContext(ownerId, value.getName(), project.getId(), phase.getId(), value.getId(),
                project.getProposal() == null ? null : project.getProposal().getId(), project.getClient().getId());
    }
}
