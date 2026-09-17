package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.HomeProjectResponse;
import com.arqly.backend.entity.Project;
import com.arqly.backend.repository.ProjectRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeProjectQueryService {
    private final ProjectRepository projects;
    public HomeProjectQueryService(ProjectRepository projects) { this.projects = projects; }
    @Transactional(readOnly = true)
    public List<HomeProjectResponse> mine(HomeUserContext context) {
        return projects.findRelevantForHome(context.tenantId(), context.userId(), context.isTenantAdmin(), PageRequest.of(0, 8)).stream()
                .map(project -> response(context, project)).toList();
    }
    private HomeProjectResponse response(HomeUserContext context, Project project) {
        boolean responsible = project.getResponsibleUser() != null && context.userId().equals(project.getResponsibleUser().getId());
        boolean manager = project.getProjectManager() != null && context.userId().equals(project.getProjectManager().getId());
        return new HomeProjectResponse(project.getId(), project.getCode(), project.getName(), project.getClient().getName(), project.getStatus(),
                null, project.getUpdatedAt(), responsible, manager, !responsible && !manager);
    }
}
