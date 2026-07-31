package com.arqly.backend.service;

import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientStatus;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ClientPortalAccessRepository;
import com.arqly.backend.repository.ProjectRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalAuthorizationService {
    private final ClientPortalAccessRepository portalRepository;
    private final ProjectRepository projectRepository;

    public PortalAuthorizationService(ClientPortalAccessRepository portalRepository, ProjectRepository projectRepository) {
        this.portalRepository = portalRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Client client(UUID token) {
        var access = portalRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Portal não encontrado."));
        if (!access.isActive() || access.isRevoked() || access.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Acesso ao portal expirado ou revogado.");
        }
        var client = access.getClient();
        if (client.isDeleted() || client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessException("Cliente indisponível.");
        }
        access.setLastAccessAt(Instant.now());
        return client;
    }

    @Transactional(readOnly = true)
    public com.arqly.backend.entity.Project project(Client client, UUID projectId) {
        return projectRepository.findByIdAndTenantIdAndClientIdAndDeletedFalse(projectId, client.getTenant().getId(), client.getId())
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado no portal."));
    }
}
