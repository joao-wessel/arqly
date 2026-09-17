package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import java.util.List;

public interface GlobalSearchProvider {
    SearchResultType type();
    List<SearchResultResponse> search(SearchUserContext context, String normalizedQuery, int limit);
}
