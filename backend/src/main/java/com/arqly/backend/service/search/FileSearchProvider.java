package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.FileResourceRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class FileSearchProvider implements GlobalSearchProvider {
    private final FileResourceRepository files;
    public FileSearchProvider(FileResourceRepository files) { this.files = files; }
    public SearchResultType type() { return SearchResultType.FILE; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return files.findAll((root, criteria, builder) -> {
                    var tag = root.join("tags", jakarta.persistence.criteria.JoinType.LEFT);
                    criteria.distinct(true);
                    return builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()),
                            builder.notEqual(root.get("status"), FileResourceStatus.DELETED), allowed(builder, criteria, root, context),
                            SearchSpecifications.matches(builder, query, root.get("name"), root.get("originalName"), root.get("extension"), tag.get("name")));
                }, PageRequest.of(0, limit)).stream()
                .map(item -> SearchResults.result(item.getId(), type(), item.getOriginalName(), item.getExtension().toUpperCase() + " • " + item.getOwnerType().name(), null, "/app/files", "File")).toList();
    }

    private jakarta.persistence.criteria.Predicate allowed(jakarta.persistence.criteria.CriteriaBuilder builder, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                            Root<com.arqly.backend.entity.FileResource> file, SearchUserContext context) {
        if (context.isTenantAdmin()) return builder.conjunction();
        Subquery<Integer> project = query.subquery(Integer.class);
        Root<Project> projectRoot = project.from(Project.class);
        project.select(builder.literal(1)).where(builder.equal(projectRoot.get("id"), file.get("ownerId")),
                builder.equal(projectRoot.get("tenant").get("id"), context.tenantId()), builder.isFalse(projectRoot.get("deleted")),
                builder.or(builder.equal(projectRoot.get("responsibleUser").get("id"), context.userId()),
                        builder.equal(projectRoot.get("projectManager").get("id"), context.userId())));
        Subquery<Integer> projectStage = query.subquery(Integer.class);
        Root<ProjectStage> projectStageRoot = projectStage.from(ProjectStage.class);
        projectStage.select(builder.literal(1)).where(builder.equal(projectStageRoot.get("projectPhase").get("project").get("id"), file.get("ownerId")),
                builder.equal(projectStageRoot.get("responsibleUser").get("id"), context.userId()), builder.isFalse(projectStageRoot.get("deleted")));
        Subquery<Integer> stage = query.subquery(Integer.class);
        Root<ProjectStage> stageRoot = stage.from(ProjectStage.class);
        var stageProject = stageRoot.join("projectPhase").join("project");
        stage.select(builder.literal(1)).where(builder.equal(stageRoot.get("id"), file.get("ownerId")), builder.isFalse(stageRoot.get("deleted")),
                builder.or(builder.equal(stageRoot.get("responsibleUser").get("id"), context.userId()),
                        builder.equal(stageProject.get("responsibleUser").get("id"), context.userId()),
                        builder.equal(stageProject.get("projectManager").get("id"), context.userId())));
        return builder.or(builder.not(file.get("ownerType").in(FileOwnerType.PROJECT, FileOwnerType.PROJECT_STAGE)),
                builder.and(builder.equal(file.get("ownerType"), FileOwnerType.PROJECT), builder.or(builder.exists(project), builder.exists(projectStage))),
                builder.and(builder.equal(file.get("ownerType"), FileOwnerType.PROJECT_STAGE), builder.exists(stage)));
    }
}
