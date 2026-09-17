package com.arqly.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.FinancialDtos.EntryResponse;
import com.arqly.backend.dto.FinancialDtos.ProposalFinancialPreview;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.FinancialEntry;
import com.arqly.backend.entity.FinancialInstallment;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.ProposalPaymentCondition;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.FinancialEntryRepository;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProposalRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProposalFinancialConversionServiceTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID proposalId = UUID.randomUUID();

    @Mock private ProposalRepository proposals;
    @Mock private FinancialEntryRepository entries;
    @Mock private FinancialInstallmentRepository installments;
    @Mock private TenantUserRepository users;
    @Mock private ProjectRepository projects;
    @Mock private ActivityEventPublisher activities;

    private ProposalFinancialConversionService service;
    private Proposal proposal;
    private TenantUser user;
    private final List<FinancialInstallment> savedInstallments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new ProposalFinancialConversionService(proposals, entries, installments, users, projects, activities);
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        Client client = new Client();
        client.setName("Cliente Silva");
        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());

        proposal = new Proposal();
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        proposal.setTenant(tenant);
        proposal.setClient(client);
        proposal.setNumber("PROP-2026-000001");
        proposal.setTitle("Projeto residencial");
        proposal.setTotal(new BigDecimal("12000.00"));
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setPaymentConditions(List.of(condition("Entrada", "4000.00", LocalDate.of(2026, 10, 1)), condition("Anteprojeto", "4000.00", LocalDate.of(2026, 11, 1)), condition("Entrega", "4000.00", LocalDate.of(2026, 12, 1))));

        user = new TenantUser();
        user.setTenant(tenant);
        user.setName("João");
        ReflectionTestUtils.setField(user, "id", userId);

        when(proposals.findByIdAndTenantIdAndDeletedFalse(proposalId, tenantId)).thenReturn(Optional.of(proposal));
        when(projects.findByProposalIdAndDeletedFalse(proposalId)).thenReturn(Optional.empty());
        when(entries.findByProposalIdAndTenantIdAndDeletedFalse(proposalId, tenantId)).thenReturn(Optional.empty());
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(entries.save(any(FinancialEntry.class))).thenAnswer(invocation -> {
            FinancialEntry entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());
            return entry;
        });
        when(installments.save(any(FinancialInstallment.class))).thenAnswer(invocation -> {
            FinancialInstallment installment = invocation.getArgument(0);
            ReflectionTestUtils.setField(installment, "id", UUID.randomUUID());
            savedInstallments.add(installment);
            return installment;
        });
        when(installments.findAllByEntryIdOrderByInstallmentNumber(any())).thenAnswer(invocation -> savedInstallments);
    }

    @Test
    void previewsApprovedProposalAndConvertsItIntoExactInstallments() {
        ProposalFinancialPreview preview = service.preview(tenantId, proposalId);
        EntryResponse generated = service.convert(tenantId, userId, proposalId);

        assertThat(preview.total()).isEqualByComparingTo("12000.00");
        assertThat(preview.installments()).hasSize(3);
        assertThat(generated.installments()).hasSize(3);
        assertThat(savedInstallments.stream().map(FinancialInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("12000.00");
        assertThat(savedInstallments).extracting(FinancialInstallment::getDueDate)
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));
        verify(entries).save(any(FinancialEntry.class));
    }

    @Test
    void preventsDuplicateFinancialGenerationForTheSameProposal() {
        when(entries.findByProposalIdAndTenantIdAndDeletedFalse(proposalId, tenantId))
                .thenReturn(Optional.of(new FinancialEntry()));

        assertThatThrownBy(() -> service.convert(tenantId, userId, proposalId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Esta proposta já possui um financeiro gerado.");
    }

    @Test
    void rejectsProposalFromAnotherTenant() {
        UUID otherTenant = UUID.randomUUID();
        when(proposals.findByIdAndTenantIdAndDeletedFalse(proposalId, otherTenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(otherTenant, proposalId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Proposta não encontrada.");
        verify(proposals).findByIdAndTenantIdAndDeletedFalse(eq(proposalId), eq(otherTenant));
    }

    private ProposalPaymentCondition condition(String description, String amount, LocalDate dueDate) {
        ProposalPaymentCondition condition = new ProposalPaymentCondition();
        condition.setDescription(description);
        condition.setValue(new BigDecimal(amount));
        condition.setDueDate(dueDate);
        return condition;
    }
}
