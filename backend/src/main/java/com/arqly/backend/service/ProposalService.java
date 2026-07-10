package com.arqly.backend.service;

import com.arqly.backend.dto.ProposalDtos.PortalProposalResponse;
import com.arqly.backend.dto.ProposalDtos.ProjectCreatedResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalDecisionRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalItemRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalItemResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalPaymentConditionRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalPaymentConditionResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalPublicClient;
import com.arqly.backend.dto.ProposalDtos.ProposalRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalStatsResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalSummaryResponse;
import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.ClientPortalAccess;
import com.arqly.backend.entity.ClientStatus;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectService;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.ProposalItem;
import com.arqly.backend.entity.ProposalPaymentCondition;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.ProposalMapper;
import com.arqly.backend.repository.ClientPortalAccessRepository;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectServiceRepository;
import com.arqly.backend.repository.ProposalItemRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.ServiceRepository;
import com.arqly.backend.repository.TenantRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class ProposalService {
    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final ProjectServiceRepository projectServiceRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final ClientRepository clientRepository;
    private final ClientPortalAccessRepository portalRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;
    private final ProposalMapper mapper;
    private final ProposalPdfService pdfService;
    private final EmailService emailService;
    private final SettingsService settingsService;

    public ProposalService(ProposalRepository proposalRepository, ProjectRepository projectRepository,
                           ProjectServiceRepository projectServiceRepository, ProposalItemRepository proposalItemRepository,
                           ClientRepository clientRepository, ClientPortalAccessRepository portalRepository,
                           ServiceRepository serviceRepository, TenantRepository tenantRepository, ProposalMapper mapper,
                           ProposalPdfService pdfService, EmailService emailService, SettingsService settingsService) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.projectServiceRepository = projectServiceRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.clientRepository = clientRepository;
        this.portalRepository = portalRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
        this.mapper = mapper;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public Page<ProposalSummaryResponse> list(UUID tenantId, String number, UUID clientId, ProposalStatus status,
                                              LocalDate from, LocalDate to, BigDecimal minValue, BigDecimal maxValue,
                                              String responsible, Pageable pageable) {
        return proposalRepository.findAll(specification(tenantId, number, clientId, status, from, to, minValue, maxValue, responsible), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ProposalResponse get(UUID tenantId, UUID id) {
        return toResponse(findTenantProposal(tenantId, id));
    }

    @Transactional
    public ProposalResponse create(UUID tenantId, String username, ProposalRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var proposal = mapper.toEntity(request);
        proposal.setTenant(tenant);
        proposal.setClient(findTenantClient(tenantId, request.clientId()));
        proposal.setNumber(nextNumber(tenantId));
        proposal.setCreatedBy(username);
        proposal.setUpdatedBy(username);
        replaceItems(tenantId, proposal, request.items());
        replacePaymentConditions(proposal, request.paymentConditions());
        recalculate(proposal);
        log.info("Creating proposal {} for tenant {}", proposal.getNumber(), tenantId);
        return toResponse(proposalRepository.save(proposal));
    }

    @Transactional
    public ProposalResponse update(UUID tenantId, UUID id, String username, ProposalRequest request) {
        var proposal = findTenantProposal(tenantId, id);
        ensureEditable(proposal);
        mapper.update(request, proposal);
        proposal.setClient(findTenantClient(tenantId, request.clientId()));
        proposal.setUpdatedBy(username);
        replaceItems(tenantId, proposal, request.items());
        replacePaymentConditions(proposal, request.paymentConditions());
        recalculate(proposal);
        log.info("Updating proposal {} for tenant {}", proposal.getNumber(), tenantId);
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse duplicate(UUID tenantId, UUID id, String username) {
        var source = findTenantProposal(tenantId, id);
        var copy = new Proposal();
        copy.setTenant(source.getTenant());
        copy.setClient(source.getClient());
        copy.setNumber(nextNumber(tenantId));
        copy.setTitle(source.getTitle() + " (cópia)");
        copy.setDescription(source.getDescription());
        copy.setValidUntil(source.getValidUntil());
        copy.setDiscount(source.getDiscount());
        copy.setAddition(source.getAddition());
        copy.setScope(source.getScope());
        copy.setExclusions(source.getExclusions());
        copy.setInternalNotes(source.getInternalNotes());
        copy.setClientNotes(source.getClientNotes());
        copy.setCreatedBy(username);
        copy.setUpdatedBy(username);
        for (ProposalItem item : source.getItems()) {
            var newItem = new ProposalItem();
            newItem.setProposal(copy);
            newItem.setService(item.getService());
            newItem.setServiceName(item.getServiceName());
            newItem.setServiceDescription(item.getServiceDescription());
            newItem.setCustomDescription(item.getCustomDescription());
            newItem.setQuantity(item.getQuantity());
            newItem.setUnit(item.getUnit());
            newItem.setUnitValue(item.getUnitValue());
            newItem.setDiscount(item.getDiscount());
            newItem.setTotal(item.getTotal());
            copy.getItems().add(newItem);
        }
        for (ProposalPaymentCondition condition : source.getPaymentConditions()) {
            var newCondition = new ProposalPaymentCondition();
            newCondition.setProposal(copy);
            newCondition.setDescription(condition.getDescription());
            newCondition.setPercentage(condition.getPercentage());
            newCondition.setValue(condition.getValue());
            newCondition.setDueDate(condition.getDueDate());
            copy.getPaymentConditions().add(newCondition);
        }
        recalculate(copy);
        return toResponse(proposalRepository.save(copy));
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var proposal = findTenantProposal(tenantId, id);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new BusinessException("Somente propostas em rascunho podem ser excluídas.");
        }
        proposal.setDeleted(true);
        proposal.setDeletedAt(Instant.now());
    }

    @Transactional
    public ProposalResponse send(UUID tenantId, UUID id, String message) {
        var proposal = findTenantProposal(tenantId, id);
        transition(proposal, ProposalStatus.SENT);
        var access = activePortalAccess(proposal.getClient());
        var link = proposalPortalUrl(access.getToken(), proposal.getId());
        var pdf = pdfService.generate(proposal);
        emailService.sendProposal(
                proposal.getClient().getEmail(),
                "Proposta " + proposal.getNumber() + " - Arqly",
                message,
                link,
                pdf,
                proposal.getNumber() + ".pdf"
        );
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse cancel(UUID tenantId, UUID id) {
        var proposal = findTenantProposal(tenantId, id);
        transition(proposal, ProposalStatus.CANCELLED);
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse expire(UUID tenantId, UUID id) {
        var proposal = findTenantProposal(tenantId, id);
        transition(proposal, ProposalStatus.EXPIRED);
        return toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(UUID tenantId, UUID id) {
        return pdfService.generate(findTenantProposal(tenantId, id));
    }

    @Transactional
    public ProjectCreatedResponse createProject(UUID tenantId, UUID proposalId) {
        var proposal = findTenantProposal(tenantId, proposalId);
        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new BusinessException("Somente propostas aceitas podem virar projeto.");
        }
        if (proposal.isProjectCreated() || projectRepository.existsByProposalIdAndDeletedFalse(proposalId)) {
            throw new BusinessException("Esta proposta já possui um projeto vinculado.");
        }
        var project = new Project();
        project.setTenant(proposal.getTenant());
        project.setClient(proposal.getClient());
        project.setProposal(proposal);
        project.setCode(nextProjectCode(tenantId));
        project.setName(proposal.getTitle());
        project.setDescription(proposal.getDescription());
        project.setInternalNotes(proposal.getScope());
        project.setContractedValue(proposal.getTotal());
        project = projectRepository.save(project);
        copyItemsToProject(proposal.getId(), project);
        proposal.setProjectCreated(true);
        return new ProjectCreatedResponse(project.getId(), proposal.getId(), project.getName(), "Projeto criado a partir da proposta.");
    }

    public ProposalStatsResponse stats(UUID tenantId) {
        var pending = proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.DRAFT)
                + proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.SENT)
                + proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.VIEWED);
        return new ProposalStatsResponse(
                proposalRepository.countByTenantIdAndDeletedFalse(tenantId),
                proposalRepository.sumTotalByTenant(tenantId),
                proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.ACCEPTED),
                proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.REJECTED),
                proposalRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProposalStatus.EXPIRED),
                pending
        );
    }

    @Transactional
    public PortalProposalResponse portalProposal(UUID token, UUID proposalId) {
        var access = validatePortalAccess(token);
        var proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
        if (proposal.isDeleted() || !proposal.getClient().getId().equals(access.getClient().getId())) {
            throw new NotFoundException("Proposta não encontrada.");
        }
        if (proposal.getStatus() == ProposalStatus.SENT) {
            transition(proposal, ProposalStatus.VIEWED);
        }
        access.setLastAccessAt(Instant.now());
        return new PortalProposalResponse(
                new ProposalPublicClient(access.getClient().getId(), displayName(access.getClient()), access.getClient().getEmail(), access.getClient().getPhone()),
                "Revise a proposta comercial e registre sua decisão.",
                toResponse(proposal)
        );
    }

    @Transactional
    public ProposalResponse accept(UUID token, UUID proposalId, ProposalDecisionRequest request, String ip, String userAgent) {
        var proposal = publicDecisionProposal(token, proposalId);
        transition(proposal, ProposalStatus.ACCEPTED);
        proposal.setAcceptedIp(ip);
        proposal.setAcceptedUserAgent(userAgent);
        if (request != null && request.note() != null && !request.note().isBlank()) {
            proposal.setClientNotes(appendNote(proposal.getClientNotes(), "Aceite: " + request.note()));
        }
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse reject(UUID token, UUID proposalId, ProposalDecisionRequest request, String ip, String userAgent) {
        var proposal = publicDecisionProposal(token, proposalId);
        transition(proposal, ProposalStatus.REJECTED);
        proposal.setRejectedIp(ip);
        proposal.setRejectedUserAgent(userAgent);
        if (request != null && request.note() != null && !request.note().isBlank()) {
            proposal.setClientNotes(appendNote(proposal.getClientNotes(), "Recusa: " + request.note()));
        }
        return toResponse(proposal);
    }

    private Specification<Proposal> specification(UUID tenantId, String number, UUID clientId, ProposalStatus status,
                                                  LocalDate from, LocalDate to, BigDecimal minValue, BigDecimal maxValue,
                                                  String responsible) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (number != null && !number.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("number")), "%" + number.toLowerCase() + "%"));
            }
            if (clientId != null) {
                predicates.add(builder.equal(root.get("client").get("id"), clientId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant()));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant()));
            }
            if (minValue != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("total"), minValue));
            }
            if (maxValue != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("total"), maxValue));
            }
            if (responsible != null && !responsible.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("createdBy")), "%" + responsible.toLowerCase() + "%"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void replaceItems(UUID tenantId, Proposal proposal, List<ProposalItemRequest> items) {
        proposal.getItems().clear();
        for (ProposalItemRequest request : items) {
            var service = serviceRepository.findByIdAndTenantIdAndDeletedFalse(request.serviceId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));
            var item = new ProposalItem();
            item.setProposal(proposal);
            item.setService(service);
            item.setServiceName(service.getName());
            item.setServiceDescription(service.getShortDescription());
            item.setCustomDescription(request.customDescription());
            item.setQuantity(scale(request.quantity()));
            item.setUnit(request.unit() == null ? service.getBillingUnit() : request.unit());
            item.setUnitValue(scale(request.unitValue() == null ? value(service.getBaseValue()) : request.unitValue()));
            item.setDiscount(scale(value(request.discount())));
            item.setTotal(lineTotal(item));
            proposal.getItems().add(item);
        }
    }

    private void replacePaymentConditions(Proposal proposal, List<ProposalPaymentConditionRequest> conditions) {
        proposal.getPaymentConditions().clear();
        if (conditions == null) return;
        for (ProposalPaymentConditionRequest request : conditions) {
            var condition = new ProposalPaymentCondition();
            condition.setProposal(proposal);
            condition.setDescription(request.description());
            condition.setPercentage(request.percentage());
            condition.setValue(scale(value(request.value())));
            condition.setDueDate(request.dueDate());
            proposal.getPaymentConditions().add(condition);
        }
    }

    private void recalculate(Proposal proposal) {
        var subtotal = proposal.getItems().stream()
                .map(ProposalItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        proposal.setSubtotal(scale(subtotal));
        proposal.setDiscount(scale(value(proposal.getDiscount())));
        proposal.setAddition(scale(value(proposal.getAddition())));
        var total = proposal.getSubtotal().subtract(proposal.getDiscount()).add(proposal.getAddition());
        if (total.signum() < 0) {
            throw new BusinessException("O total da proposta não pode ser negativo.");
        }
        proposal.setTotal(scale(total));
        for (ProposalPaymentCondition condition : proposal.getPaymentConditions()) {
            if ((condition.getValue() == null || condition.getValue().signum() == 0) && condition.getPercentage() != null) {
                condition.setValue(scale(proposal.getTotal().multiply(condition.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
            }
        }
    }

    private void transition(Proposal proposal, ProposalStatus target) {
        var current = proposal.getStatus();
        boolean allowed = switch (target) {
            case SENT -> current == ProposalStatus.DRAFT;
            case VIEWED -> current == ProposalStatus.SENT;
            case ACCEPTED, REJECTED -> current == ProposalStatus.SENT || current == ProposalStatus.VIEWED;
            case EXPIRED -> current == ProposalStatus.SENT || current == ProposalStatus.VIEWED;
            case CANCELLED -> current == ProposalStatus.DRAFT || current == ProposalStatus.SENT || current == ProposalStatus.VIEWED;
            case DRAFT -> false;
        };
        if (!allowed) {
            throw new BusinessException("Transição de status inválida.");
        }
        proposal.setStatus(target);
        var now = Instant.now();
        switch (target) {
            case SENT -> proposal.setSentAt(now);
            case VIEWED -> proposal.setViewedAt(now);
            case ACCEPTED -> proposal.setAcceptedAt(now);
            case REJECTED -> proposal.setRejectedAt(now);
            case EXPIRED -> proposal.setExpiredAt(now);
            case CANCELLED -> proposal.setCancelledAt(now);
            case DRAFT -> { }
        }
    }

    private Proposal publicDecisionProposal(UUID token, UUID proposalId) {
        var access = validatePortalAccess(token);
        var proposal = proposalRepository.findById(proposalId).orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
        if (proposal.isDeleted() || !proposal.getClient().getId().equals(access.getClient().getId())) {
            throw new NotFoundException("Proposta não encontrada.");
        }
        if (proposal.getStatus() == ProposalStatus.SENT) {
            transition(proposal, ProposalStatus.VIEWED);
        }
        access.setLastAccessAt(Instant.now());
        return proposal;
    }

    private ClientPortalAccess validatePortalAccess(UUID token) {
        var access = portalRepository.findByToken(token).orElseThrow(() -> new NotFoundException("Portal não encontrado."));
        if (!access.isActive() || access.isRevoked() || access.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Acesso ao portal expirado ou revogado.");
        }
        var client = access.getClient();
        if (client.isDeleted() || client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessException("Cliente indisponível.");
        }
        return access;
    }

    private ClientPortalAccess activePortalAccess(Client client) {
        return portalRepository.findByClientIdAndActiveTrueAndRevokedFalse(client.getId())
                .filter(access -> access.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessException("Gere um acesso ativo ao portal do cliente antes de enviar a proposta."));
    }

    private Proposal findTenantProposal(UUID tenantId, UUID id) {
        return proposalRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Proposta não encontrada."));
    }

    private Client findTenantClient(UUID tenantId, UUID id) {
        return clientRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    private void ensureEditable(Proposal proposal) {
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new BusinessException("Somente propostas em rascunho podem ser editadas.");
        }
    }

    private String nextNumber(UUID tenantId) {
        var prefix = "PROP-" + Year.now().getValue() + "-";
        return proposalRepository.findTopByTenantIdAndNumberStartingWithOrderByNumberDesc(tenantId, prefix)
                .map(Proposal::getNumber)
                .map(number -> number.substring(number.lastIndexOf('-') + 1))
                .map(Integer::parseInt)
                .map(value -> value + 1)
                .map(value -> prefix + "%06d".formatted(value))
                .orElse(prefix + "000001");
    }

    private String nextProjectCode(UUID tenantId) {
        var prefix = "PRJ-" + Year.now().getValue() + "-";
        return projectRepository.findTopByTenantIdAndCodeStartingWithOrderByCodeDesc(tenantId, prefix)
                .map(Project::getCode)
                .map(code -> code.substring(code.lastIndexOf('-') + 1))
                .map(Integer::parseInt)
                .map(value -> value + 1)
                .map(value -> prefix + "%06d".formatted(value))
                .orElse(prefix + "000001");
    }

    private void copyItemsToProject(UUID proposalId, Project project) {
        for (ProposalItem item : proposalItemRepository.findAllByProposalIdOrderByCreatedAtAsc(proposalId)) {
            var service = new ProjectService();
            service.setProject(project);
            service.setName(item.getServiceName());
            service.setDescription(firstNotBlank(item.getCustomDescription(), item.getServiceDescription()));
            service.setQuantity(value(item.getQuantity()));
            service.setUnit(item.getUnit());
            service.setContractedValue(value(item.getTotal()));
            projectServiceRepository.save(service);
        }
    }

    private BigDecimal lineTotal(ProposalItem item) {
        var total = item.getQuantity().multiply(item.getUnitValue()).subtract(item.getDiscount());
        if (total.signum() < 0) {
            throw new BusinessException("O total do item não pode ser negativo.");
        }
        return scale(total);
    }

    private ProposalSummaryResponse toSummary(Proposal proposal) {
        return new ProposalSummaryResponse(proposal.getId(), proposal.getNumber(), proposal.getClient().getId(),
                displayName(proposal.getClient()), proposal.getTitle(), proposal.getTotal(), proposal.getStatus(),
                proposal.getValidUntil(), proposal.getCreatedBy(), proposal.isProjectCreated(), proposal.getCreatedAt(), proposal.getUpdatedAt());
    }

    private ProposalResponse toResponse(Proposal proposal) {
        var client = proposal.getClient();
        return new ProposalResponse(proposal.getId(), proposal.getNumber(), client.getId(), displayName(client), client.getEmail(),
                proposal.getTitle(), proposal.getDescription(), proposal.getValidUntil(), proposal.getSubtotal(),
                proposal.getDiscount(), proposal.getAddition(), proposal.getTotal(), proposal.getStatus(), proposal.getScope(),
                proposal.getExclusions(), proposal.getInternalNotes(), proposal.getClientNotes(), proposal.getCreatedBy(),
                proposal.getUpdatedBy(), proposal.getSentAt(), proposal.getViewedAt(), proposal.getAcceptedAt(), proposal.getRejectedAt(),
                proposal.isProjectCreated(), proposal.getItems().stream().map(this::toItemResponse).toList(),
                proposal.getPaymentConditions().stream().map(this::toPaymentResponse).toList(), proposal.getCreatedAt(), proposal.getUpdatedAt());
    }

    private ProposalItemResponse toItemResponse(ProposalItem item) {
        return new ProposalItemResponse(item.getId(), item.getService() == null ? null : item.getService().getId(),
                item.getServiceName(), item.getServiceDescription(), item.getCustomDescription(), item.getQuantity(),
                item.getUnit(), item.getUnitValue(), item.getDiscount(), item.getTotal());
    }

    private ProposalPaymentConditionResponse toPaymentResponse(ProposalPaymentCondition condition) {
        return new ProposalPaymentConditionResponse(condition.getId(), condition.getDescription(), condition.getPercentage(),
                condition.getValue(), condition.getDueDate());
    }

    private String proposalPortalUrl(UUID token, UUID proposalId) {
        return settingsService.getGeneral().publicUrl() + "/portal/" + token + "/proposals/" + proposalId;
    }

    private String appendNote(String current, String note) {
        return (current == null || current.isBlank()) ? note : current + "\n\n" + note;
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }
}
