package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.BriefingRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class BriefingSearchProvider implements GlobalSearchProvider {
    private final BriefingRepository briefings;
    public BriefingSearchProvider(BriefingRepository briefings) { this.briefings = briefings; }
    public SearchResultType type() { return SearchResultType.BRIEFING; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return briefings.findAll((root, criteria, builder) -> builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                        SearchSpecifications.matches(builder, query, root.get("title"), root.get("description"), root.get("client").get("name"), root.get("client").get("legalName"))), PageRequest.of(0, limit))
                .stream().map(item -> SearchResults.result(item.getId(), type(), item.getTitle(), item.getClient().getName(), item.getDescription(), "/app/briefings", "ClipboardList")).toList();
    }
}
