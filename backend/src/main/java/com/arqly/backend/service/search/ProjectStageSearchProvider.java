package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.ProjectStageRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ProjectStageSearchProvider implements GlobalSearchProvider {
    private final ProjectStageRepository stages;
    public ProjectStageSearchProvider(ProjectStageRepository stages) { this.stages = stages; }
    public SearchResultType type() { return SearchResultType.PROJECT_STAGE; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return stages.findAll((root, criteria, builder) -> {
                    var project = root.join("projectPhase").join("project");
                    return builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                            builder.isFalse(project.get("deleted")), ProjectSearchAccess.allowed(builder, criteria, project, context),
                            SearchSpecifications.matches(builder, query, root.get("name"), root.get("description"), project.get("name")));
                }, PageRequest.of(0, limit)).stream()
                .map(item -> SearchResults.result(item.getId(), type(), item.getName(), item.getProjectPhase().getProject().getName(), item.getDescription(), "/app/projects/" + item.getProjectPhase().getProject().getId() + "/stages/" + item.getId(), "ListTodo")).toList();
    }
}
