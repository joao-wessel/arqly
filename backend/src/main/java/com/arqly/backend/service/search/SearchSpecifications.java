package com.arqly.backend.service.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.text.Normalizer;
import java.util.Arrays;

final class SearchSpecifications {
    private SearchSpecifications() {}

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    static Predicate matches(CriteriaBuilder builder, String query, Path<String>... fields) {
        Expression<String> pattern = builder.literal("%" + query + "%");
        return builder.or(Arrays.stream(fields)
                .map(field -> builder.like(unaccent(builder, field), pattern))
                .toArray(Predicate[]::new));
    }

    static Expression<String> unaccent(CriteriaBuilder builder, Expression<String> value) {
        return builder.function("unaccent", String.class, builder.lower(builder.coalesce(value, "")));
    }
}
