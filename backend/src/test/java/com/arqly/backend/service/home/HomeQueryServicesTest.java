package com.arqly.backend.service.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.dto.CalendarDtos.CalendarItemResponse;
import com.arqly.backend.dto.FinancialDtos.CashFlowResponse;
import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityContent;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.entity.Role;
import com.arqly.backend.entity.FinancialEntry;
import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.entity.FinancialInstallment;
import com.arqly.backend.entity.FinancialInstallmentStatus;
import com.arqly.backend.repository.ActivityRepository;
import com.arqly.backend.repository.ApprovalRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import com.arqly.backend.service.FinancialService;
import com.arqly.backend.service.calendar.CalendarQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeQueryServicesTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final HomeUserContext admin = new HomeUserContext(tenantId, userId, Set.of(Role.ROLE_TENANT_ADMIN));
    private final HomeUserContext commonUser = new HomeUserContext(tenantId, userId, Set.of(Role.ROLE_USER));

    @Mock private CalendarQueryService calendar;
    @Mock private ProjectRepository projects;
    @Mock private ProjectStageRepository stages;
    @Mock private ApprovalRepository approvals;
    @Mock private FinancialService financial;
    @Mock private ActivityRepository activities;
    @Mock private FinancialInstallmentRepository installments;

    @Test
    void attentionProvidersAreAggregatedAndLimitedWithoutCrossTenantInput() {
        AttentionProvider first = context -> List.of(item("OVERDUE_STAGE", "HIGH"));
        AttentionProvider second = context -> List.of(item("PENDING_APPROVAL", "CRITICAL"));
        AttentionQueryService service = new AttentionQueryService(List.of(first, second));

        var result = service.list(commonUser);

        assertThat(result).extracting(AttentionItemResponse::type).containsExactly("PENDING_APPROVAL", "OVERDUE_STAGE");
    }

    @Test
    void returnsOnlyTodayCalendarItemsForAuthenticatedUser() {
        CalendarItemResponse event = new CalendarItemResponse(UUID.randomUUID(), null, UUID.randomUUID(), "Visita", LocalDate.now().toString(), null,
                true, "VISIT", "SCHEDULED", null, null, null, null, userId, "João", null, false, false, "/app/calendar", "#000");
        when(calendar.query(eq(tenantId), eq(LocalDate.now()), eq(LocalDate.now()), any())).thenReturn(List.of(event));

        var result = new HomeTodayQueryService(calendar).today(commonUser);

        assertThat(result).containsExactly(event);
        verify(calendar).query(eq(tenantId), eq(LocalDate.now()), eq(LocalDate.now()), any());
    }

    @Test
    void returnsProjectsRelatedToCurrentUser() {
        Project project = project();
        when(projects.findRelevantForHome(eq(tenantId), eq(userId), eq(false), any())).thenReturn(List.of(project));

        var result = new HomeProjectQueryService(projects).mine(commonUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(project.getId());
        assertThat(result.get(0).responsible()).isTrue();
    }

    @Test
    void returnsGeneralIndicatorsForAdmin() {
        when(projects.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStatus.IN_PROGRESS)).thenReturn(3L);
        when(stages.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProjectStageStatus.IN_PROGRESS)).thenReturn(8L);
        when(stages.countByTenantIdAndStatusNotAndPlannedEndBeforeAndDeletedFalse(eq(tenantId), eq(ProjectStageStatus.COMPLETED), any())).thenReturn(2L);
        when(approvals.findTop20ByTenantIdAndStatusAndDeletedFalseOrderByDeadlineAsc(any(), any())).thenReturn(List.of(new com.arqly.backend.entity.Approval()));

        var result = new HomeIndicatorsQueryService(projects, stages, approvals).get(admin);

        assertThat(result.activeProjects()).isEqualTo(3);
        assertThat(result.inProgressStages()).isEqualTo(8);
        assertThat(result.overdueStages()).isEqualTo(2);
        assertThat(result.pendingApprovals()).isEqualTo(1);
    }

    @Test
    void restrictsFinancialSummaryToTenantAdmins() {
        when(financial.cashFlow(tenantId)).thenReturn(new CashFlowResponse(new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("800"), new BigDecimal("50")));
        HomeFinancialQueryService service = new HomeFinancialQueryService(financial);

        assertThat(service.get(admin).expectedBalance()).isEqualByComparingTo("800");
        assertThatThrownBy(() -> service.get(commonUser)).isInstanceOf(com.arqly.backend.exception.BusinessException.class);
    }

    @Test
    void showsOverdueFinancialAttentionOnlyToTenantAdminsAndUsesTenantScope() {
        FinancialEntry receivable = new FinancialEntry();
        receivable.setType(FinancialEntryType.RECEIVABLE);
        receivable.setDescription("Projeto residencial");
        FinancialInstallment installment = new FinancialInstallment();
        ReflectionTestUtils.setField(installment, "id", UUID.randomUUID());
        installment.setEntry(receivable);
        installment.setStatus(FinancialInstallmentStatus.OPEN);
        installment.setDueDate(LocalDate.now().minusDays(1));
        when(installments.findAllByEntryTenantIdAndEntryDeletedFalse(tenantId)).thenReturn(List.of(installment));

        var provider = new OverdueReceivablesAttentionProvider(installments);

        assertThat(provider.provide(commonUser)).isEmpty();
        assertThat(provider.provide(admin)).extracting(AttentionItemResponse::type).containsExactly("OVERDUE_RECEIVABLE");
        verify(installments).findAllByEntryTenantIdAndEntryDeletedFalse(tenantId);
    }

    @Test
    void returnsRecentActivitiesScopedToTenant() {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", UUID.randomUUID());
        activity.setAuthorName("João");
        activity.setType(ActivityType.PROJECT_UPDATED);
        ActivityContent content = new ActivityContent();
        content.setTitle("Projeto atualizado");
        content.setDescription("Atualizou o projeto");
        activity.setContent(content);
        when(activities.findTop20ByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(activity));

        var result = new HomeActivityQueryService(activities).recent(commonUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Projeto atualizado");
        verify(activities).findTop20ByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
    }

    private AttentionItemResponse item(String type, String severity) {
        return new AttentionItemResponse(type, severity, type, type, null, null, UUID.randomUUID(), "/app", null);
    }

    private Project project() {
        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());
        project.setCode("PRJ-001");
        project.setName("Residência");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        com.arqly.backend.entity.Client client = new com.arqly.backend.entity.Client();
        client.setName("Cliente");
        project.setClient(client);
        com.arqly.backend.entity.TenantUser responsible = new com.arqly.backend.entity.TenantUser();
        ReflectionTestUtils.setField(responsible, "id", userId);
        project.setResponsibleUser(responsible);
        return project;
    }
}
