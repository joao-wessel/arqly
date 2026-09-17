package com.arqly.backend.service.file;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ConstructionDiaryEntryRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConstructionDiaryFileOwnerResolver implements FileOwnerResolver {
    private final ConstructionDiaryEntryRepository repository;
    public ConstructionDiaryFileOwnerResolver(ConstructionDiaryEntryRepository repository) { this.repository = repository; }
    public FileOwnerType type() { return FileOwnerType.CONSTRUCTION_DIARY_ENTRY; }
    @Transactional(readOnly = true)
    public FileOwnerContext resolve(UUID tenantId, UUID ownerId) {
        var entry = repository.findByIdAndTenantIdAndDeletedFalse(ownerId, tenantId).orElseThrow(() -> new NotFoundException("Registro do Diário não encontrado."));
        return new FileOwnerContext(ownerId, entry.getProject().getName() + " · " + entry.getTitle(), entry.getProject().getId(), null, null,
                entry.getProject().getProposal() == null ? null : entry.getProject().getProposal().getId(), entry.getProject().getClient().getId());
    }
}
