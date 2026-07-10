package com.arqly.backend.service;

import com.arqly.backend.dto.ClientDtos.ClientPortalAccessResponse;
import com.arqly.backend.dto.ClientDtos.ClientPortalProposalResponse;
import com.arqly.backend.dto.ClientDtos.ClientPublicResponse;
import com.arqly.backend.dto.ClientDtos.ClientRequest;
import com.arqly.backend.dto.ClientDtos.ClientResponse;
import com.arqly.backend.dto.ClientDtos.ClientSummaryResponse;
import com.arqly.backend.dto.ClientDtos.PortalPublicResponse;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.ClientPortalAccess;
import com.arqly.backend.entity.ClientStatus;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.ClientMapper;
import com.arqly.backend.repository.ClientPortalAccessRepository;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final ClientPortalAccessRepository portalRepository;
    private final ProposalRepository proposalRepository;
    private final TenantRepository tenantRepository;
    private final ClientMapper mapper;
    private final SettingsService settingsService;

    public ClientService(ClientRepository clientRepository, ClientPortalAccessRepository portalRepository,
                         ProposalRepository proposalRepository, TenantRepository tenantRepository, ClientMapper mapper,
                         SettingsService settingsService) {
        this.clientRepository = clientRepository;
        this.portalRepository = portalRepository;
        this.proposalRepository = proposalRepository;
        this.tenantRepository = tenantRepository;
        this.mapper = mapper;
        this.settingsService = settingsService;
    }

    public Page<ClientSummaryResponse> list(UUID tenantId, String name, String document, ClientStatus status, String city,
                                            ClientPersonType personType, Boolean portal, Pageable pageable) {
        return clientRepository.findAll(specification(tenantId, name, document, status, city, personType, portal), pageable)
                .map(this::toSummary);
    }

    public ClientResponse get(UUID tenantId, UUID id) {
        return toResponse(findTenantClient(tenantId, id));
    }

    @Transactional
    public ClientResponse create(UUID tenantId, ClientRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var client = mapper.toEntity(request);
        client.setTenant(tenant);
        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(UUID tenantId, UUID id, ClientRequest request) {
        var client = findTenantClient(tenantId, id);
        mapper.update(request, client);
        return toResponse(client);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var client = findTenantClient(tenantId, id);
        client.setDeleted(true);
        client.setDeletedAt(Instant.now());
        portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(client.getId()).ifPresent(this::revoke);
    }

    @Transactional
    public ClientPortalAccessResponse generatePortalAccess(UUID tenantId, UUID clientId) {
        var client = findTenantClient(tenantId, clientId);
        portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(clientId).ifPresent(this::revoke);

        var access = new ClientPortalAccess();
        access.setClient(client);
        access.setToken(UUID.randomUUID());
        access.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        access.setActive(true);
        access.setRevoked(false);
        return toPortalResponse(portalRepository.save(access));
    }

    @Transactional
    public ClientPortalAccessResponse revokePortalAccess(UUID tenantId, UUID clientId) {
        findTenantClient(tenantId, clientId);
        var access = portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(clientId)
                .orElseThrow(() -> new NotFoundException("Acesso ativo não encontrado."));
        revoke(access);
        return toPortalResponse(access);
    }

    @Transactional
    public ClientPortalAccessResponse updatePortalValidity(UUID tenantId, UUID clientId, Instant expiresAt) {
        findTenantClient(tenantId, clientId);
        var access = portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(clientId)
                .orElseThrow(() -> new NotFoundException("Acesso ativo não encontrado."));
        if (expiresAt.isBefore(Instant.now())) {
            throw new BusinessException("A validade deve ser futura.");
        }
        access.setExpiresAt(expiresAt);
        return toPortalResponse(access);
    }

    public List<ClientPortalAccessResponse> portalHistory(UUID tenantId, UUID clientId) {
        findTenantClient(tenantId, clientId);
        return portalRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toPortalResponse)
                .toList();
    }

    @Transactional
    public PortalPublicResponse portal(UUID token) {
        var access = portalRepository.findByToken(token).orElseThrow(() -> new NotFoundException("Portal não encontrado."));
        if (!access.isActive() || access.isRevoked() || access.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Acesso ao portal expirado ou revogado.");
        }
        var client = access.getClient();
        if (client.isDeleted() || client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessException("Cliente indisponível.");
        }
        access.setLastAccessAt(Instant.now());
        var proposals = proposalRepository.findAllByClientIdAndDeletedFalseAndStatusInOrderByCreatedAtDesc(
                        client.getId(),
                        List.of(ProposalStatus.SENT, ProposalStatus.VIEWED, ProposalStatus.ACCEPTED)
                ).stream()
                .map(proposal -> new ClientPortalProposalResponse(
                        proposal.getId(),
                        proposal.getNumber(),
                        proposal.getTitle(),
                        proposal.getStatus(),
                        proposal.getTotal(),
                        proposal.getValidUntil(),
                        settingsService.getGeneral().publicUrl() + "/portal/" + token + "/proposals/" + proposal.getId(),
                        proposal.getCreatedAt()
                ))
                .toList();
        return new PortalPublicResponse(
                new ClientPublicResponse(client.getId(), displayName(client), client.getEmail(), client.getPhone(), client.getCity(), client.getState()),
                List.of(),
                proposals,
                "Bem-vindo ao portal do cliente Arqly."
        );
    }

    private Specification<Client> specification(UUID tenantId, String name, String document, ClientStatus status, String city,
                                                ClientPersonType personType, Boolean portal) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (name != null && !name.isBlank()) {
                String term = "%" + name.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), term),
                        builder.like(builder.lower(root.get("legalName")), term),
                        builder.like(builder.lower(root.get("tradeName")), term)
                ));
            }
            if (document != null && !document.isBlank()) {
                String term = "%" + document.replaceAll("\\D", "") + "%";
                predicates.add(builder.or(builder.like(root.get("cpf"), term), builder.like(root.get("cnpj"), term)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            }
            if (personType != null) {
                predicates.add(builder.equal(root.get("personType"), personType));
            }
            if (portal != null) {
                var subquery = query.subquery(UUID.class);
                var access = subquery.from(ClientPortalAccess.class);
                subquery.select(access.get("client").get("id"));
                subquery.where(
                        builder.equal(access.get("client").get("id"), root.get("id")),
                        builder.isTrue(access.get("active")),
                        builder.isFalse(access.get("revoked")),
                        builder.greaterThan(access.get("expiresAt"), Instant.now())
                );
                predicates.add(portal ? builder.exists(subquery) : builder.not(builder.exists(subquery)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Client findTenantClient(UUID tenantId, UUID id) {
        return clientRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    private void revoke(ClientPortalAccess access) {
        access.setActive(false);
        access.setRevoked(true);
    }

    private ClientSummaryResponse toSummary(Client client) {
        var portal = portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(client.getId())
                .filter(access -> access.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
        return new ClientSummaryResponse(client.getId(), client.getPersonType(), displayName(client), document(client),
                client.getEmail(), client.getPhone(), client.getCity(), client.getState(), client.getStatus(), portal,
                client.getCreatedAt(), client.getUpdatedAt());
    }

    private ClientResponse toResponse(Client client) {
        var portalAccess = portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(client.getId())
                .map(this::toPortalResponse)
                .orElse(null);
        return new ClientResponse(client.getId(), client.getPersonType(), client.getName(), client.getCpf(), client.getRg(),
                client.getBirthDate(), client.getLegalName(), client.getTradeName(), client.getCnpj(), client.getStateRegistration(),
                client.getEmail(), client.getPhone(), client.getWhatsapp(), client.getZipCode(), client.getStreet(),
                client.getNumber(), client.getComplement(), client.getDistrict(), client.getCity(), client.getState(),
                client.getNotes(), client.getStatus(), client.getCreatedAt(), client.getUpdatedAt(), portalAccess);
    }

    private ClientPortalAccessResponse toPortalResponse(ClientPortalAccess access) {
        return new ClientPortalAccessResponse(access.getId(), access.getToken(), portalUrl(access.getToken()), access.getCreatedAt(),
                access.getExpiresAt(), access.isRevoked(), access.getLastAccessAt(), access.isActive());
    }

    private String portalUrl(UUID token) {
        return settingsService.getGeneral().publicUrl() + "/portal/" + token;
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }

    private String document(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getCpf() : client.getCnpj();
    }
}
