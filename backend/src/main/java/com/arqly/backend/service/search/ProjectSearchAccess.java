package com.arqly.backend.service.search;

import com.arqly.backend.entity.ProjectStage;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class ProjectSearchAccess {
    private ProjectSearchAccess() {}

    static Predicate allowed(CriteriaBuilder builder, jakarta.persistence.criteria.CriteriaQuery<?> query,
                             From<?, ?> project, SearchUserContext context) {
        if (context.isTenantAdmin()) return builder.conjunction();
        Subquery<Integer> stage = query.subquery(Integer.class);
        Root<ProjectStage> stageRoot = stage.from(ProjectStage.class);
        stage.select(builder.literal(1)).where(
                builder.equal(stageRoot.get("projectPhase").get("project").get("id"), project.get("id")),
                builder.equal(stageRoot.get("responsibleUser").get("id"), context.userId()),
                builder.isFalse(stageRoot.get("deleted")));
        return builder.or(
                builder.equal(project.get("responsibleUser").get("id"), context.userId()),
                builder.equal(project.get("projectManager").get("id"), context.userId()),
                builder.exists(stage));
    }
}
