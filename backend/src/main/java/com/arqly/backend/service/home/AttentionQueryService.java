package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttentionQueryService {
    private final List<AttentionProvider> providers;

    public AttentionQueryService(List<AttentionProvider> providers) {
        this.providers = providers;
    }

    @Transactional(readOnly = true)
    public List<AttentionItemResponse> list(HomeUserContext context) {
        return providers.stream()
                .flatMap(provider -> provider.provide(context).stream())
                .sorted(Comparator.comparingInt((AttentionItemResponse item) -> priority(item.severity())).reversed()
                        .thenComparing(AttentionItemResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    private int priority(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }
}
