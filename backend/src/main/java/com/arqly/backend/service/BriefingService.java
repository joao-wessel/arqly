package com.arqly.backend.service;

import com.arqly.backend.dto.BriefingDtos.BriefingRequest;
import com.arqly.backend.dto.BriefingDtos.BriefingRequirementRequest;
import com.arqly.backend.dto.BriefingDtos.BriefingRequirementResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingStatsResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingSummaryResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.entity.Briefing;
import com.arqly.backend.entity.BriefingRequirement;
import com.arqly.backend.entity.BriefingStatus;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.OriginType;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.BriefingRepository;
import com.arqly.backend.repository.BriefingRequirementRepository;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.ProjectTemplateRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BriefingService {
    private final BriefingRepository briefingRepository;
    private final BriefingRequirementRepository requirementRepository;
    private final ProposalRepository proposalRepository;
    private final ClientRepository clientRepository;
    private final ProjectTemplateRepository templateRepository;
    private final TenantUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ProposalService proposalService;

    public BriefingService(BriefingRepository briefingRepository,
                           BriefingRequirementRepository requirementRepository,
                           ProposalRepository proposalRepository,
                           ClientRepository clientRepository,
                           ProjectTemplateRepository templateRepository,
                           TenantUserRepository userRepository,
                           TenantRepository tenantRepository,
                           ProposalService proposalService) {
        this.briefingRepository = briefingRepository;
        this.requirementRepository = requirementRepository;
        this.proposalRepository = proposalRepository;
        this.clientRepository = clientRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.proposalService = proposalService;
    }

    @Transactional(readOnly = true)
    public Page<BriefingSummaryResponse> list(UUID tenantId, String search, UUID clientId, UUID responsibleUserId,
                                              BriefingStatus status, Pageable pageable) {
        return briefingRepository.findAll(specification(tenantId, search, clientId, responsibleUserId, status), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public BriefingResponse get(UUID tenantId, UUID id) {
        return toResponse(findBriefing(tenantId, id));
    }

    @Transactional
    public BriefingResponse create(UUID tenantId, String username, BriefingRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var briefing = new Briefing();
        briefing.setTenant(tenant);
        briefing.setCreatedBy(username);
        fill(tenantId, briefing, request, username);
        replaceRequirements(briefing, request.requirements());
        return toResponse(briefingRepository.save(briefing));
    }

    @Transactional
    public BriefingResponse update(UUID tenantId, UUID id, String username, BriefingRequest request) {
        var briefing = findBriefing(tenantId, id);
        fill(tenantId, briefing, request, username);
        replaceRequirements(briefing, request.requirements());
        return toResponse(briefing);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var briefing = findBriefing(tenantId, id);
        if (proposalRepository.findByBriefingIdAndTenantIdAndDeletedFalse(id, tenantId).isPresent()) {
            throw new BusinessException("Briefings com proposta vinculada não podem ser excluídos.");
        }
        briefing.setDeleted(true);
        briefing.setDeletedAt(Instant.now());
    }

    @Transactional
    public ProposalResponse generateProposal(UUID tenantId, UUID id, String username) {
        var briefing = findBriefing(tenantId, id);
        proposalRepository.findByBriefingIdAndTenantIdAndDeletedFalse(id, tenantId).ifPresent(existing -> {
            throw new BusinessException("Este briefing já possui uma proposta vinculada.");
        });
        var proposal = new Proposal();
        proposal.setTenant(briefing.getTenant());
        proposal.setClient(briefing.getClient());
        proposal.setBriefing(briefing);
        proposal.setOriginType(OriginType.BRIEFING);
        proposal.setNumber(nextNumber(tenantId));
        proposal.setTitle(briefing.getTitle());
        proposal.setDescription(briefing.getDescription());
        proposal.setScope(scopeFromBriefing(briefing));
        proposal.setInternalNotes(briefing.getPreferenceNotes());
        proposal.setClientNotes(briefing.getRestrictionNotes());
        proposal.setSubtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        proposal.setDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        proposal.setAddition(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        proposal.setTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        proposal.setCreatedBy(username);
        proposal.setUpdatedBy(username);
        briefing.setStatus(BriefingStatus.COMPLETED);
        return proposalService.toResponse(proposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public BriefingStatsResponse stats(UUID tenantId) {
        return new BriefingStatsResponse(
                briefingRepository.count((root, query, builder) -> builder.and(
                        builder.equal(root.get("tenant").get("id"), tenantId),
                        builder.isFalse(root.get("deleted"))
                )),
                briefingRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, BriefingStatus.IN_PROGRESS),
                briefingRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, BriefingStatus.COMPLETED),
                proposalRepository.countByTenantIdAndBriefingIsNotNullAndDeletedFalse(tenantId)
        );
    }

    private void fill(UUID tenantId, Briefing briefing, BriefingRequest request, String username) {
        briefing.setClient(findClient(tenantId, request.clientId()));
        briefing.setProjectTemplate(request.projectTemplateId() == null ? null : templateRepository.findByIdAndTenantIdAndDeletedFalse(request.projectTemplateId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Modelo não encontrado.")));
        briefing.setResponsibleUser(request.responsibleUserId() == null ? null : userRepository.findByIdAndTenantId(request.responsibleUserId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário responsável não encontrado.")));
        briefing.setTitle(request.title());
        briefing.setDescription(request.description());
        briefing.setStatus(request.status() == null ? BriefingStatus.DRAFT : request.status());
        briefing.setApproximateArea(scale(request.approximateArea()));
        briefing.setWorkAddress(request.workAddress());
        briefing.setCity(request.city());
        briefing.setState(request.state());
        briefing.setDesiredDeadline(request.desiredDeadline());
        briefing.setExpectedBudget(scale(request.expectedBudget()));
        briefing.setArchitecturalStyle(request.architecturalStyle());
        briefing.setColorPalette(request.colorPalette());
        briefing.setDesiredMaterials(request.desiredMaterials());
        briefing.setPreferenceNotes(request.preferenceNotes());
        briefing.setLegalRestrictions(request.legalRestrictions());
        briefing.setTechnicalRestrictions(request.technicalRestrictions());
        briefing.setClientRestrictions(request.clientRestrictions());
        briefing.setRestrictionNotes(request.restrictionNotes());
        briefing.setUpdatedBy(username);
    }

    private void replaceRequirements(Briefing briefing, List<BriefingRequirementRequest> requests) {
        briefing.getRequirements().clear();
        if (requests == null) return;
        for (BriefingRequirementRequest request : requests) {
            var requirement = new BriefingRequirement();
            requirement.setTenant(briefing.getTenant());
            requirement.setBriefing(briefing);
            requirement.setDescription(request.description());
            requirement.setOrder(request.order());
            briefing.getRequirements().add(requirement);
        }
    }

    private Specification<Briefing> specification(UUID tenantId, String search, UUID clientId, UUID responsibleUserId, BriefingStatus status) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (search != null && !search.isBlank()) {
                var term = "%" + search.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), term),
                        builder.like(builder.lower(root.get("description")), term)
                ));
            }
            if (clientId != null) predicates.add(builder.equal(root.get("client").get("id"), clientId));
            if (responsibleUserId != null) predicates.add(builder.equal(root.get("responsibleUser").get("id"), responsibleUserId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Briefing findBriefing(UUID tenantId, UUID id) {
        return briefingRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Briefing não encontrado."));
    }

    private Client findClient(UUID tenantId, UUID id) {
        return clientRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    private BriefingSummaryResponse toSummary(Briefing briefing) {
        var proposal = proposalRepository.findByBriefingIdAndTenantIdAndDeletedFalse(briefing.getId(), briefing.getTenant().getId()).orElse(null);
        return new BriefingSummaryResponse(briefing.getId(), briefing.getClient().getId(), displayName(briefing.getClient()),
                briefing.getProjectTemplate() == null ? null : briefing.getProjectTemplate().getId(),
                briefing.getProjectTemplate() == null ? null : briefing.getProjectTemplate().getName(),
                briefing.getResponsibleUser() == null ? null : briefing.getResponsibleUser().getId(),
                briefing.getResponsibleUser() == null ? null : briefing.getResponsibleUser().getName(),
                briefing.getTitle(), briefing.getStatus(), proposal != null, proposal == null ? null : proposal.getId(),
                proposal == null ? null : proposal.getNumber(), briefing.getCreatedAt(), briefing.getUpdatedAt());
    }

    private BriefingResponse toResponse(Briefing briefing) {
        var proposal = proposalRepository.findByBriefingIdAndTenantIdAndDeletedFalse(briefing.getId(), briefing.getTenant().getId()).orElse(null);
        var requirements = requirementRepository.findAllByBriefingIdAndTenantIdAndDeletedFalseOrderByOrderAsc(briefing.getId(), briefing.getTenant().getId()).stream()
                .map(item -> new BriefingRequirementResponse(item.getId(), item.getDescription(), item.getOrder(), item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
        return new BriefingResponse(briefing.getId(), briefing.getClient().getId(), displayName(briefing.getClient()),
                briefing.getProjectTemplate() == null ? null : briefing.getProjectTemplate().getId(),
                briefing.getProjectTemplate() == null ? null : briefing.getProjectTemplate().getName(),
                briefing.getResponsibleUser() == null ? null : briefing.getResponsibleUser().getId(),
                briefing.getResponsibleUser() == null ? null : briefing.getResponsibleUser().getName(),
                briefing.getTitle(), briefing.getDescription(), briefing.getStatus(), briefing.getApproximateArea(),
                briefing.getWorkAddress(), briefing.getCity(), briefing.getState(), briefing.getDesiredDeadline(),
                briefing.getExpectedBudget(), briefing.getArchitecturalStyle(), briefing.getColorPalette(),
                briefing.getDesiredMaterials(), briefing.getPreferenceNotes(), briefing.getLegalRestrictions(),
                briefing.getTechnicalRestrictions(), briefing.getClientRestrictions(), briefing.getRestrictionNotes(),
                proposal != null, proposal == null ? null : proposal.getId(), proposal == null ? null : proposal.getNumber(),
                requirements, briefing.getCreatedAt(), briefing.getUpdatedAt());
    }

    private String scopeFromBriefing(Briefing briefing) {
        var requirements = requirementRepository.findAllByBriefingIdAndTenantIdAndDeletedFalseOrderByOrderAsc(briefing.getId(), briefing.getTenant().getId()).stream()
                .map(BriefingRequirement::getDescription)
                .toList();
        var lines = new ArrayList<String>();
        if (briefing.getProjectTemplate() != null) lines.add("Modelo: " + briefing.getProjectTemplate().getName());
        if (briefing.getApproximateArea() != null) lines.add("Área aproximada: " + briefing.getApproximateArea() + " m²");
        if (briefing.getWorkAddress() != null && !briefing.getWorkAddress().isBlank()) lines.add("Endereço da obra: " + briefing.getWorkAddress());
        if (!requirements.isEmpty()) lines.add("Programa de necessidades: " + String.join("; ", requirements));
        return String.join("\n", lines);
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

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }
}
