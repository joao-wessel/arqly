package com.arqly.backend.dto;

import com.arqly.backend.entity.BillingUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ServiceCatalogDtos {
    private ServiceCatalogDtos() {}

    public record ServiceCategoryRequest(
            @NotBlank String name,
            String description,
            String color,
            String icon,
            boolean active
    ) {}

    public record ServiceCategoryResponse(
            UUID id,
            String name,
            String description,
            String color,
            String icon,
            boolean active,
            long servicesCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ServiceRequest(
            @NotBlank String name,
            UUID categoryId,
            @Size(max = 500) String shortDescription,
            String fullDescription,
            @DecimalMin(value = "0.00") BigDecimal baseValue,
            @NotBlank @Size(min = 3, max = 8) String currency,
            @NotNull BillingUnit billingUnit,
            boolean active,
            boolean featured
    ) {}

    public record ServiceSummaryResponse(
            UUID id,
            String name,
            UUID categoryId,
            String categoryName,
            String categoryColor,
            BigDecimal baseValue,
            String currency,
            BillingUnit billingUnit,
            boolean active,
            boolean featured,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ServiceResponse(
            UUID id,
            String name,
            UUID categoryId,
            String categoryName,
            String shortDescription,
            String fullDescription,
            BigDecimal baseValue,
            String currency,
            BillingUnit billingUnit,
            boolean active,
            boolean featured,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ServiceCatalogStatsResponse(
            long services,
            long categories,
            long activeServices,
            long inactiveServices
    ) {}
}
