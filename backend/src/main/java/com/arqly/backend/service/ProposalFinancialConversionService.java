package com.arqly.backend.service;

import com.arqly.backend.dto.FinancialDtos.EntryResponse;
import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.FinancialDtos.ProposalFinancialPreview;
import com.arqly.backend.entity.FinancialEntry;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.FinancialEntryStatus;
import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.entity.FinancialInstallment;
import com.arqly.backend.entity.FinancialInstallmentStatus;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.FinancialEntryRepository;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalFinancialConversionService {
    private final ProposalRepository proposals;
    private final FinancialEntryRepository entries;
    private final FinancialInstallmentRepository installments;
    private final TenantUserRepository users;
    private final ProjectRepository projects;
    private final ActivityEventPublisher activities;

    public ProposalFinancialConversionService(ProposalRepository proposals, FinancialEntryRepository entries, FinancialInstallmentRepository installments, TenantUserRepository users, ProjectRepository projects, ActivityEventPublisher activities) {
        this.proposals = proposals; this.entries = entries; this.installments = installments; this.users = users; this.projects = projects; this.activities = activities;
    }

    @Transactional(readOnly = true)
    public ProposalFinancialPreview preview(UUID tenantId, UUID proposalId) {
        Proposal proposal = proposal(tenantId, proposalId);
        List<com.arqly.backend.dto.FinancialDtos.InstallmentResponse> items = conditions(proposal).stream().map((condition) -> new com.arqly.backend.dto.FinancialDtos.InstallmentResponse(null, condition.number, condition.description, condition.amount, BigDecimal.ZERO, condition.amount, condition.dueDate, "OPEN", false)).toList();
        var project = projects.findByProposalIdAndDeletedFalse(proposalId).orElse(null);
        return new ProposalFinancialPreview(proposal.getId(), proposal.getNumber(), proposal.getClient().getName(), project == null ? null : project.getName(), "Proposta " + proposal.getNumber() + " — " + proposal.getTitle(), proposal.getTotal(), items);
    }

    @Transactional
    public EntryResponse convert(UUID tenantId, UUID userId, UUID proposalId) {
        Proposal proposal = proposal(tenantId, proposalId);
        if (entries.findByProposalIdAndTenantIdAndDeletedFalse(proposalId, tenantId).isPresent()) throw new BusinessException("Esta proposta já possui um financeiro gerado.");
        TenantUser user = users.findByIdAndTenantId(userId, tenantId).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        FinancialEntry entry = new FinancialEntry(); entry.setTenant(user.getTenant()); entry.setProposal(proposal); entry.setClient(proposal.getClient()); entry.setProject(projects.findByProposalIdAndDeletedFalse(proposalId).orElse(null)); entry.setType(FinancialEntryType.RECEIVABLE); entry.setDescription("Proposta " + proposal.getNumber() + " — " + proposal.getTitle()); entry.setTotalAmount(proposal.getTotal()); entry.setIssueDate(LocalDate.now()); entry.setCompetenceDate(LocalDate.now()); entry.setStatus(FinancialEntryStatus.OPEN); entry.setCreatedBy(user); entry.setClientVisible(false); entries.save(entry);
        for (var condition : conditions(proposal)) create(entry, condition.number, condition.description, condition.amount, condition.dueDate);
        activities.publishFile(tenantId, entry.getProject() == null ? null : entry.getProject().getId(), null, null, proposalId, proposal.getClient().getId(), userId, user.getName(), ActivityType.FINANCIAL_CREATED_FROM_PROPOSAL, "Financeiro gerado", "Conta a receber criada a partir da proposta " + proposal.getNumber() + ".", null);
        return response(entry);
    }
    private Proposal proposal(UUID tenantId, UUID proposalId) { Proposal proposal = proposals.findByIdAndTenantIdAndDeletedFalse(proposalId, tenantId).orElseThrow(() -> new NotFoundException("Proposta não encontrada.")); if (proposal.getStatus() != ProposalStatus.ACCEPTED) throw new BusinessException("Apenas propostas aceitas podem gerar financeiro."); return proposal; }
    private List<Condition> conditions(Proposal proposal) { if (proposal.getPaymentConditions().isEmpty()) return List.of(new Condition(1, proposal.getTitle(), proposal.getTotal(), LocalDate.now())); List<Condition> result = new java.util.ArrayList<>(); for (int index = 0; index < proposal.getPaymentConditions().size(); index++) { var c = proposal.getPaymentConditions().get(index); if (c.getDueDate() == null) throw new BusinessException("Informe os vencimentos das condições de pagamento antes de gerar o financeiro."); result.add(new Condition(index + 1, c.getDescription(), c.getValue(), c.getDueDate())); } if (result.stream().map(Condition::amount).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(proposal.getTotal()) != 0) throw new BusinessException("A soma das parcelas deve corresponder ao valor total da proposta."); return result; }
    private record Condition(int number, String description, BigDecimal amount, LocalDate dueDate) {}
    private void create(FinancialEntry entry, int number, String description, BigDecimal amount, LocalDate dueDate) { FinancialInstallment installment = new FinancialInstallment(); installment.setEntry(entry); installment.setInstallmentNumber(number); installment.setDescription(description); installment.setAmount(amount); installment.setDueDate(dueDate); installment.setStatus(FinancialInstallmentStatus.OPEN); installment.setSettledAmount(BigDecimal.ZERO); installments.save(installment); }
    private EntryResponse response(FinancialEntry entry) { List<com.arqly.backend.dto.FinancialDtos.InstallmentResponse> list = installments.findAllByEntryIdOrderByInstallmentNumber(entry.getId()).stream().map(i -> new com.arqly.backend.dto.FinancialDtos.InstallmentResponse(i.getId(), i.getInstallmentNumber(), i.getDescription(), i.getAmount(), i.getSettledAmount(), i.getAmount().subtract(i.getSettledAmount()), i.getDueDate(), i.getStatus().name(), false)).toList(); return new EntryResponse(entry.getId(), entry.getType(), entry.getDescription(), entry.getClient().getName(), null, null, entry.getTotalAmount(), BigDecimal.ZERO, entry.getTotalAmount(), entry.getStatus(), entry.getIssueDate(), entry.isClientVisible(), list); }
}
