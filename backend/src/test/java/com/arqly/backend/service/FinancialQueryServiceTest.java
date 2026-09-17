package com.arqly.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.arqly.backend.dto.FinancialDtos.ClientFinancialSummary;
import com.arqly.backend.dto.FinancialDtos.ProjectFinancialSummary;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.FinancialEntry;
import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.entity.FinancialInstallment;
import com.arqly.backend.entity.FinancialInstallmentStatus;
import com.arqly.backend.entity.Project;
import com.arqly.backend.repository.ClientRepository;
import com.arqly.backend.repository.FinancialEntryRepository;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import com.arqly.backend.repository.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FinancialQueryServiceTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock private FinancialEntryRepository entries;
    @Mock private FinancialInstallmentRepository installments;
    @Mock private ProjectRepository projects;
    @Mock private ClientRepository clients;
    private FinancialQueryService service;
    private Project project;
    private Client client;

    @BeforeEach
    void setUp() {
        service = new FinancialQueryService(entries, installments, projects, clients);
        project = new Project();
        ReflectionTestUtils.setField(project, "id", projectId);
        client = new Client();
        ReflectionTestUtils.setField(client, "id", clientId);
        when(projects.findByIdAndTenantIdAndDeletedFalse(projectId, tenantId)).thenReturn(Optional.of(project));
        when(clients.findByIdAndTenantIdAndDeletedFalse(clientId, tenantId)).thenReturn(Optional.of(client));
    }

    @Test
    void calculatesProjectAndClientSummariesFromEntriesAndInstallments() {
        FinancialEntry receivable = entry(FinancialEntryType.RECEIVABLE, project, client);
        FinancialEntry payable = entry(FinancialEntryType.PAYABLE, project, client);
        when(entries.findAllByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(receivable, payable));
        when(installments.findAllByEntryIdOrderByInstallmentNumber(receivable.getId())).thenReturn(List.of(installment(receivable, "10000.00", "4000.00", LocalDate.now().minusDays(1))));
        when(installments.findAllByEntryIdOrderByInstallmentNumber(payable.getId())).thenReturn(List.of(installment(payable, "3000.00", "1000.00", LocalDate.now().plusDays(3))));

        ProjectFinancialSummary projectSummary = service.project(tenantId, projectId);
        ClientFinancialSummary clientSummary = service.client(tenantId, clientId);

        assertThat(projectSummary.expectedIncome()).isEqualByComparingTo("10000.00");
        assertThat(projectSummary.actualIncome()).isEqualByComparingTo("4000.00");
        assertThat(projectSummary.expectedExpense()).isEqualByComparingTo("3000.00");
        assertThat(projectSummary.actualExpense()).isEqualByComparingTo("1000.00");
        assertThat(projectSummary.expectedResult()).isEqualByComparingTo("7000.00");
        assertThat(projectSummary.actualResult()).isEqualByComparingTo("3000.00");
        assertThat(clientSummary.totalExpected()).isEqualByComparingTo("10000.00");
        assertThat(clientSummary.totalReceived()).isEqualByComparingTo("4000.00");
        assertThat(clientSummary.openAmount()).isEqualByComparingTo("6000.00");
        assertThat(clientSummary.overdueAmount()).isEqualByComparingTo("6000.00");
    }

    private FinancialEntry entry(FinancialEntryType type, Project linkedProject, Client linkedClient) {
        FinancialEntry entry = new FinancialEntry();
        ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());
        entry.setType(type);
        entry.setProject(linkedProject);
        entry.setClient(linkedClient);
        return entry;
    }

    private FinancialInstallment installment(FinancialEntry entry, String amount, String settled, LocalDate dueDate) {
        FinancialInstallment installment = new FinancialInstallment();
        installment.setEntry(entry);
        installment.setAmount(new BigDecimal(amount));
        installment.setSettledAmount(new BigDecimal(settled));
        installment.setDueDate(dueDate);
        installment.setStatus(new BigDecimal(settled).compareTo(new BigDecimal(amount)) == 0 ? FinancialInstallmentStatus.PAID : FinancialInstallmentStatus.PARTIALLY_PAID);
        return installment;
    }
}
