package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.GeneratedDocumentRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class DocumentSearchProvider implements GlobalSearchProvider {
    private final GeneratedDocumentRepository documents;
    public DocumentSearchProvider(GeneratedDocumentRepository documents) { this.documents = documents; }
    public SearchResultType type() { return SearchResultType.DOCUMENT; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return documents.findAll((root, criteria, builder) -> {
                    var project = root.join("project", jakarta.persistence.criteria.JoinType.LEFT);
                    var access = builder.or(builder.isNull(project.get("id")), ProjectSearchAccess.allowed(builder, criteria, project, context));
                    return builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                            builder.isTrue(root.get("currentVersion")), access,
                            SearchSpecifications.matches(builder, query, root.get("title"), root.get("template").get("name"), root.get("client").get("name"), project.get("name")));
                }, PageRequest.of(0, limit)).stream()
                .map(item -> SearchResults.result(item.getId(), type(), item.getTitle(), "Versão " + item.getVersion(), item.getProject() == null ? item.getClient().getName() : item.getProject().getName(), "/app/documents/generated", "FileText")).toList();
    }
}
