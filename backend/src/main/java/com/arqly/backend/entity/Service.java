package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "services")
public class Service extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String fullDescription;

    private BigDecimal baseValue;

    @Column(nullable = false, length = 8)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingUnit billingUnit = BillingUnit.PROJECT;

    private boolean active = true;
    private boolean featured = false;
    private boolean deleted = false;
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public ServiceCategory getCategory() { return category; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }
    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BillingUnit getBillingUnit() { return billingUnit; }
    public void setBillingUnit(BillingUnit billingUnit) { this.billingUnit = billingUnit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
