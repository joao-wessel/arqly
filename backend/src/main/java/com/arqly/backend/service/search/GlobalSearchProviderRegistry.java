package com.arqly.backend.service.search;

import com.arqly.backend.entity.SearchResultType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GlobalSearchProviderRegistry {
    private final Map<SearchResultType, GlobalSearchProvider> providers;

    public GlobalSearchProviderRegistry(List<GlobalSearchProvider> providers) {
        Map<SearchResultType, GlobalSearchProvider> mapped = new EnumMap<>(SearchResultType.class);
        providers.forEach(provider -> mapped.put(provider.type(), provider));
        this.providers = Map.copyOf(mapped);
    }

    public List<GlobalSearchProvider> providersFor(List<SearchResultType> types) {
        if (types == null || types.isEmpty()) return providers.values().stream().toList();
        return types.stream().distinct().map(providers::get).filter(java.util.Objects::nonNull).toList();
    }
}
