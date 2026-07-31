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
@Table(name = "project_stage_templates")
public class ProjectStageTemplate extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_phase_template_id", nullable = false)
    private ProjectPhaseTemplate projectPhaseTemplate;

    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "display_order", nullable = false)
    private int order;
    private String color;
    private String icon;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercentage = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressCalculationMode progressCalculationMode = ProgressCalculationMode.MANUAL;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public ProjectPhaseTemplate getProjectPhaseTemplate() { return projectPhaseTemplate; }
    public void setProjectPhaseTemplate(ProjectPhaseTemplate projectPhaseTemplate) { this.projectPhaseTemplate = projectPhaseTemplate; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public BigDecimal getWeightPercentage() { return weightPercentage; }
    public void setWeightPercentage(BigDecimal weightPercentage) { this.weightPercentage = weightPercentage; }
    public ProgressCalculationMode getProgressCalculationMode() { return progressCalculationMode; }
    public void setProgressCalculationMode(ProgressCalculationMode progressCalculationMode) { this.progressCalculationMode = progressCalculationMode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
