package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.ProjectRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ProjectSearchProvider implements GlobalSearchProvider {
    private final ProjectRepository projects;
    public ProjectSearchProvider(ProjectRepository projects) { this.projects = projects; }
    public SearchResultType type() { return SearchResultType.PROJECT; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return projects.findAll((root, criteria, builder) -> builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                        ProjectSearchAccess.allowed(builder, criteria, root, context),
                        SearchSpecifications.matches(builder, query, root.get("name"), root.get("description"), root.get("client").get("name"), root.get("client").get("legalName"))), PageRequest.of(0, limit))
                .stream().map(item -> SearchResults.result(item.getId(), type(), item.getName(), item.getClient().getName() + " • " + status(item.getStatus().name()), item.getDescription(), "/app/projects/" + item.getId(), "FolderKanban")).toList();
    }
    private String status(String value) { return switch (value) { case "IN_PROGRESS" -> "Em andamento"; case "COMPLETED" -> "Concluído"; case "ON_HOLD" -> "Pausado"; default -> "Planejamento"; }; }
}
