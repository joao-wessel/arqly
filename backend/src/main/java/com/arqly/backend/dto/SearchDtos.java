package com.arqly.backend.dto;

import com.arqly.backend.entity.SearchResultType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SearchDtos {
    private SearchDtos() {}

    public record SearchResultResponse(UUID id, SearchResultType type, String title, String subtitle,
                                       String description, String actionUrl, String icon,
                                       Map<String, String> metadata) {}

    public record SearchResponse(List<SearchResultResponse> results, boolean hasMore) {}
}
