package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.HomeIndicatorsResponse;
import com.arqly.backend.entity.ApprovalStatus;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.repository.ApprovalRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeIndicatorsQueryService {
    private final ProjectRepository projects;
    private final ProjectStageRepository stages;
    private final ApprovalRepository approvals;
    public HomeIndicatorsQueryService(ProjectRepository projects, ProjectStageRepository stages, ApprovalRepository approvals) {
        this.projects = projects; this.stages = stages; this.approvals = approvals;
    }
    @Transactional(readOnly = true)
    public HomeIndicatorsResponse get(HomeUserContext context) {
        if (context.isTenantAdmin()) {
            return new HomeIndicatorsResponse(
                    projects.countByTenantIdAndStatusAndDeletedFalse(context.tenantId(), ProjectStatus.IN_PROGRESS),
                    stages.countByTenantIdAndStatusAndDeletedFalse(context.tenantId(), ProjectStageStatus.IN_PROGRESS),
                    stages.countByTenantIdAndStatusNotAndPlannedEndBeforeAndDeletedFalse(context.tenantId(), ProjectStageStatus.COMPLETED, LocalDate.now()),
                    approvals.findTop20ByTenantIdAndStatusAndDeletedFalseOrderByDeadlineAsc(context.tenantId(), ApprovalStatus.PENDING).size());
        }
        var mine = projects.findRelevantForHome(context.tenantId(), context.userId(), false, org.springframework.data.domain.PageRequest.of(0, 100));
        long active = mine.stream().filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS).count();
        var projectIds = mine.stream().map(project -> project.getId()).collect(java.util.stream.Collectors.toSet());
        if (projectIds.isEmpty()) return new HomeIndicatorsResponse(active, 0, 0, 0);
        var allStages = stages.findAllForHomeByProjectIds(context.tenantId(), projectIds);
        var overdue = stages.findOverdueForHome(context.tenantId(), LocalDate.now(), List.of(ProjectStageStatus.COMPLETED, ProjectStageStatus.CANCELLED)).stream()
                .filter(stage -> projectIds.contains(stage.getProjectPhase().getProject().getId())).toList();
        long inProgress = allStages.stream().filter(stage -> stage.getStatus() == ProjectStageStatus.IN_PROGRESS).count();
        long pending = approvals.findTop20ByTenantIdAndStatusAndDeletedFalseOrderByDeadlineAsc(context.tenantId(), ApprovalStatus.PENDING).stream()
                .filter(approval -> projectIds.contains(approval.getProject().getId())).count();
        return new HomeIndicatorsResponse(active, inProgress, overdue.size(), pending);
    }
}
