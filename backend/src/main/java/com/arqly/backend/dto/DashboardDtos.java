package com.arqly.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record AdminDashboard(long tenantCount, long userCount, long activeTenantCount,
                                 List<AccessItem> latestAccesses, List<TenantItem> latestTenants) {}

    public record AccessItem(String name, String email, Instant lastAccessAt, String scope) {}
    public record TenantItem(UUID id, String tradeName, String primaryEmail, Instant createdAt) {}
    public record TenantDashboard(List<Card> cards) {}
    public record Card(String title, String description, String icon) {}
}
