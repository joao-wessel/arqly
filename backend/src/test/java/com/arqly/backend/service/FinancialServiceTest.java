package com.arqly.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.dto.FinancialDtos.CashFlowResponse;
import com.arqly.backend.dto.FinancialDtos.EntryRequest;
import com.arqly.backend.dto.FinancialDtos.SettlementRequest;
import com.arqly.backend.entity.FinancialEntry;
import com.arqly.backend.entity.FinancialEntryStatus;
import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.entity.FinancialInstallment;
import com.arqly.backend.entity.FinancialInstallmentStatus;
import com.arqly.backend.entity.FinancialPeriodicity;
import com.arqly.backend.entity.FinancialSettlement;
import com.arqly.backend.entity.PaymentMethod;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.FinancialCategoryRepository;
import com.arqly.backend.repository.FinancialEntryRepository;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import com.arqly.backend.repository.FinancialSettlementRepository;
import com.arqly.backend.repository.ProjectRepository;
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
class FinancialServiceTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();
    private final UUID installmentId = UUID.randomUUID();

    @Mock private FinancialEntryRepository entries;
    @Mock private FinancialInstallmentRepository installments;
    @Mock private FinancialSettlementRepository settlements;
    @Mock private FinancialCategoryRepository categories;
    @Mock private TenantUserRepository users;
    @Mock private ClientRepository clients;
    @Mock private ProjectRepository projects;
    @Mock private ActivityEventPublisher activities;

    private FinancialService service;
    private FinancialEntry entry;
    private FinancialInstallment installment;
    private final List<FinancialSettlement> savedSettlements = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new FinancialService(entries, installments, settlements, categories, users, clients, projects, activities);
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantUser user = new TenantUser();
        user.setTenant(tenant);
        user.setName("João");
        ReflectionTestUtils.setField(user, "id", userId);
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        entry = new FinancialEntry();
        ReflectionTestUtils.setField(entry, "id", entryId);
        entry.setTenant(tenant);
        entry.setType(FinancialEntryType.RECEIVABLE);
        entry.setDescription("Projeto residencial");
        entry.setTotalAmount(new BigDecimal("5000.00"));
        entry.setStatus(FinancialEntryStatus.OPEN);
        entry.setIssueDate(LocalDate.of(2026, 10, 1));

        installment = new FinancialInstallment();
        ReflectionTestUtils.setField(installment, "id", installmentId);
        installment.setEntry(entry);
        installment.setInstallmentNumber(1);
        installment.setAmount(new BigDecimal("5000.00"));
        installment.setSettledAmount(BigDecimal.ZERO);
        installment.setDueDate(LocalDate.of(2026, 10, 10));
        installment.setStatus(FinancialInstallmentStatus.OPEN);

        when(installments.findByIdAndEntryTenantIdAndEntryDeletedFalse(installmentId, tenantId)).thenReturn(Optional.of(installment));
        when(installments.findAllByEntryIdOrderByInstallmentNumber(entryId)).thenReturn(List.of(installment));
        when(settlements.findAllByInstallmentIdOrderBySettlementDateAsc(installmentId)).thenAnswer(invocation -> savedSettlements);
        when(settlements.save(any(FinancialSettlement.class))).thenAnswer(invocation -> {
            FinancialSettlement settlement = invocation.getArgument(0);
            ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());
            savedSettlements.add(settlement);
            return settlement;
        });
    }

    @Test
    void supportsPartialThenTotalSettlementAndMultipleSettlements() {
        service.settle(tenantId, userId, installmentId, settlement("2000.00"));
        assertThat(installment.getSettledAmount()).isEqualByComparingTo("2000.00");
        assertThat(installment.getStatus()).isEqualTo(FinancialInstallmentStatus.PARTIALLY_PAID);
        assertThat(entry.getStatus()).isEqualTo(FinancialEntryStatus.PARTIALLY_SETTLED);

        service.settle(tenantId, userId, installmentId, settlement("3000.00"));
        assertThat(savedSettlements).hasSize(2);
        assertThat(installment.getSettledAmount()).isEqualByComparingTo("5000.00");
        assertThat(installment.getStatus()).isEqualTo(FinancialInstallmentStatus.PAID);
        assertThat(entry.getStatus()).isEqualTo(FinancialEntryStatus.SETTLED);
    }

    @Test
    void rejectsSettlementAboveRemainingBalance() {
        savedSettlements.add(existingSettlement("4000.00"));
        installment.setSettledAmount(new BigDecimal("4000.00"));
        installment.setStatus(FinancialInstallmentStatus.PARTIALLY_PAID);

        assertThatThrownBy(() -> service.settle(tenantId, userId, installmentId, settlement("2000.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor da baixa não pode ser maior que o saldo da parcela.");
    }

    @Test
    void reversesSettlementOnlyWithReasonAndRecalculatesBalance() {
        FinancialSettlement settlement = existingSettlement("5000.00");
        savedSettlements.add(settlement);
        installment.setSettledAmount(new BigDecimal("5000.00"));
        installment.setStatus(FinancialInstallmentStatus.PAID);
        entry.setStatus(FinancialEntryStatus.SETTLED);
        UUID settlementId = settlement.getId();
        when(settlements.findByIdAndInstallmentEntryTenantId(settlementId, tenantId)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.reverse(tenantId, userId, settlementId, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe o motivo do estorno.");

        service.reverse(tenantId, userId, settlementId, "Pagamento devolvido");
        assertThat(settlement.getReversalReason()).isEqualTo("Pagamento devolvido");
        assertThat(installment.getSettledAmount()).isEqualByComparingTo("0.00");
        assertThat(installment.getStatus()).isEqualTo(FinancialInstallmentStatus.OPEN);
        assertThat(entry.getStatus()).isEqualTo(FinancialEntryStatus.OPEN);
    }

    @Test
    void cancelsOnlyEntryWithoutSettlements() {
        when(entries.findByIdAndTenantIdAndDeletedFalse(entryId, tenantId)).thenReturn(Optional.of(entry));
        service.cancel(tenantId, userId, entryId);

        assertThat(entry.getStatus()).isEqualTo(FinancialEntryStatus.CANCELLED);
        assertThat(installment.getStatus()).isEqualTo(FinancialInstallmentStatus.CANCELLED);
    }

    @Test
    void preventsCancellationWhenThereAreSettlements() {
        installment.setSettledAmount(new BigDecimal("1.00"));
        when(entries.findByIdAndTenantIdAndDeletedFalse(entryId, tenantId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.cancel(tenantId, userId, entryId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser cancelada diretamente");
    }

    @Test
    void createsManualPayableWithExactInstallmentsAndCashFlow() {
        List<FinancialInstallment> generated = new ArrayList<>();
        when(entries.save(any(FinancialEntry.class))).thenAnswer(invocation -> {
            FinancialEntry saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", entryId);
            return saved;
        });
        when(installments.save(any(FinancialInstallment.class))).thenAnswer(invocation -> {
            FinancialInstallment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            generated.add(saved);
            return saved;
        });
        when(installments.findAllByEntryIdOrderByInstallmentNumber(entryId)).thenAnswer(invocation -> generated);

        service.create(tenantId, userId, new EntryRequest(FinancialEntryType.PAYABLE, "Software", null, null, null, "Fornecedor", new BigDecimal("900.00"), LocalDate.of(2026, 10, 1), null, 3, LocalDate.of(2026, 10, 10), FinancialPeriodicity.MONTHLY, null, false));

        assertThat(generated).hasSize(3);
        assertThat(generated.stream().map(FinancialInstallment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("900.00");
        generated.get(0).setSettledAmount(new BigDecimal("300.00"));
        generated.get(0).setStatus(FinancialInstallmentStatus.PAID);
        when(entries.findAllByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(entry));
        entry.setType(FinancialEntryType.PAYABLE);
        entry.setStatus(FinancialEntryStatus.OPEN);
        entry.setTotalAmount(new BigDecimal("900.00"));
        CashFlowResponse cashFlow = service.cashFlow(tenantId);
        assertThat(cashFlow.expectedExpense()).isEqualByComparingTo("600.00");
        assertThat(cashFlow.actualExpense()).isEqualByComparingTo("300.00");
    }

    @Test
    void doesNotExposeEntryFromAnotherTenant() {
        UUID otherTenant = UUID.randomUUID();
        when(entries.findByIdAndTenantIdAndDeletedFalse(entryId, otherTenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(otherTenant, entryId)).isInstanceOf(com.arqly.backend.exception.NotFoundException.class);
        verify(entries).findByIdAndTenantIdAndDeletedFalse(eq(entryId), eq(otherTenant));
    }

    private SettlementRequest settlement(String amount) {
        return new SettlementRequest(new BigDecimal(amount), LocalDate.of(2026, 10, 5), PaymentMethod.PIX, "Recebimento");
    }

    private FinancialSettlement existingSettlement(String amount) {
        FinancialSettlement settlement = new FinancialSettlement();
        ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());
        settlement.setInstallment(installment);
        settlement.setAmount(new BigDecimal(amount));
        settlement.setSettlementDate(LocalDate.of(2026, 10, 5));
        settlement.setPaymentMethod(PaymentMethod.PIX);
        return settlement;
    }
}
