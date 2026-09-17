package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResponse;
import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {
    private static final int MINIMUM_QUERY_LENGTH = 2;
    private static final int DEFAULT_LIMIT = 12;
    private static final int MAXIMUM_LIMIT = 100;
    private final GlobalSearchProviderRegistry registry;

    public GlobalSearchService(GlobalSearchProviderRegistry registry) { this.registry = registry; }

    @Transactional(readOnly = true)
    public SearchResponse search(SearchUserContext context, String query, Integer requestedLimit, List<SearchResultType> types) {
        String normalized = SearchSpecifications.normalize(query);
        if (normalized.length() < MINIMUM_QUERY_LENGTH) return new SearchResponse(List.of(), false);
        int limit = Math.min(Math.max(requestedLimit == null ? DEFAULT_LIMIT : requestedLimit, 1), MAXIMUM_LIMIT);
        int perProvider = Math.max(3, Math.min(limit, 50));
        List<SearchResultResponse> all = registry.providersFor(types).stream()
                .flatMap(provider -> provider.search(context, normalized, perProvider).stream())
                .sorted(Comparator.<SearchResultResponse>comparingInt(item -> rank(item, normalized))
                        .thenComparing(SearchResultResponse::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new SearchResponse(all.stream().limit(limit).toList(), all.size() > limit);
    }

    private int rank(SearchResultResponse result, String query) {
        return rankValue(result.title(), query) * 3 + rankValue(result.subtitle(), query) * 2 + rankValue(result.description(), query);
    }

    private int rankValue(String value, String query) {
        String normalized = SearchSpecifications.normalize(value);
        if (normalized.equals(query)) return 0;
        if (normalized.startsWith(query)) return 1;
        return normalized.contains(query) ? 2 : 3;
    }
}
