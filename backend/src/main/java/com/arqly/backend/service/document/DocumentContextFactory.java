package com.arqly.backend.service.document;

import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentContextFactory {
    private final TenantRepository tenantRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;

    public DocumentContextFactory(TenantRepository tenantRepository, ClientRepository clientRepository,
                                  ProjectRepository projectRepository, ProposalRepository proposalRepository) {
        this.tenantRepository = tenantRepository;
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
    }

    public DocumentContext create(UUID tenantId, UUID projectId, UUID proposalId, UUID clientId) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        Project project = projectId == null ? null : projectRepository.findByIdAndTenantIdAndDeletedFalse(projectId, tenantId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
        Proposal proposal = proposalId == null ? null : proposalRepository.findByIdAndTenantIdAndDeletedFalse(proposalId, tenantId)
                .orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
        Client client = clientId == null ? null : clientRepository.findByIdAndTenantIdAndDeletedFalse(clientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

        if (project != null) {
            validateSameClient(client, project.getClient());
            client = project.getClient();
            if (proposal == null) proposal = project.getProposal();
            if (proposal != null && project.getProposal() != null && !project.getProposal().getId().equals(proposal.getId())) {
                throw new BusinessException("A proposta informada não pertence ao projeto.");
            }
        }
        if (proposal != null) {
            validateSameClient(client, proposal.getClient());
            client = proposal.getClient();
        }
        if (client == null) throw new BusinessException("Informe um cliente, uma proposta ou um projeto.");

        return new DocumentContext(tenant, client, project, proposal,
                project == null ? null : project.getResponsibleUser(), LocalDate.now());
    }

    private void validateSameClient(Client selected, Client expected) {
        if (selected != null && !selected.getId().equals(expected.getId())) {
            throw new BusinessException("As entidades informadas pertencem a clientes diferentes.");
        }
    }
}
