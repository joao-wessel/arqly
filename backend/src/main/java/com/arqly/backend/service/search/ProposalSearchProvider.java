package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.ProposalRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ProposalSearchProvider implements GlobalSearchProvider {
    private final ProposalRepository proposals;
    public ProposalSearchProvider(ProposalRepository proposals) { this.proposals = proposals; }
    public SearchResultType type() { return SearchResultType.PROPOSAL; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return proposals.findAll((root, criteria, builder) -> builder.and(builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                        SearchSpecifications.matches(builder, query, root.get("number"), root.get("title"), root.get("client").get("name"), root.get("client").get("legalName"))), PageRequest.of(0, limit))
                .stream().map(item -> SearchResults.result(item.getId(), type(), item.getNumber() + " — " + item.getTitle(), item.getClient().getName(), null, "/app/proposals", "FileText")).toList();
    }
}
