package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import com.arqly.backend.entity.ProjectStatus;
import com.arqly.backend.repository.ProjectRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StaleProjectsAttentionProvider implements AttentionProvider {
    private static final long DAYS_WITHOUT_UPDATE = 14;
    private final ProjectRepository projects;
    public StaleProjectsAttentionProvider(ProjectRepository projects) { this.projects = projects; }
    public List<AttentionItemResponse> provide(HomeUserContext context) {
        Instant cutoff = Instant.now().minus(DAYS_WITHOUT_UPDATE, ChronoUnit.DAYS);
        return projects.findTop20ByTenantIdAndDeletedFalseAndUpdatedAtBeforeOrderByUpdatedAtAsc(context.tenantId(), cutoff).stream()
                .filter(project -> project.getStatus() != ProjectStatus.COMPLETED && project.getStatus() != ProjectStatus.CANCELLED)
                .filter(project -> HomeAccess.canSee(context, project)).limit(5)
                .map(project -> new AttentionItemResponse("STALE_PROJECT", "NORMAL", "Projeto sem atualização recente", project.getName(),
                        project.getId(), project.getName(), project.getId(), "/app/projects/" + project.getId(), null)).toList();
    }
}
