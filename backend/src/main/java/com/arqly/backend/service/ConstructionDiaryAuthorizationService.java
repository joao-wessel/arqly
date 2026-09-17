package com.arqly.backend.service;

import com.arqly.backend.entity.ConstructionDiaryEntry;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ConstructionDiaryEntryRepository;
import com.arqly.backend.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConstructionDiaryAuthorizationService {
    private final ConstructionDiaryEntryRepository entryRepository;
    private final ProjectRepository projectRepository;
    public ConstructionDiaryAuthorizationService(ConstructionDiaryEntryRepository entryRepository, ProjectRepository projectRepository) { this.entryRepository = entryRepository; this.projectRepository = projectRepository; }
    public ConstructionDiaryEntry entry(UUID tenantId, UUID id) { return entryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElseThrow(() -> new NotFoundException("Registro do Diário não encontrado.")); }
    public com.arqly.backend.entity.Project project(UUID tenantId, UUID id) { return projectRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElseThrow(() -> new NotFoundException("Projeto não encontrado.")); }
    public void projectMatches(ConstructionDiaryEntry entry, UUID projectId) { if (!entry.getProject().getId().equals(projectId)) throw new BusinessException("O registro não pertence ao projeto informado."); }
}
