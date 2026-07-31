package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "briefing_requirements")
public class BriefingRequirement extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "briefing_id", nullable = false)
    private Briefing briefing;

    @Column(nullable = false)
    private String description;
    @Column(name = "display_order", nullable = false)
    private int order;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Briefing getBriefing() { return briefing; }
    public void setBriefing(Briefing briefing) { this.briefing = briefing; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
