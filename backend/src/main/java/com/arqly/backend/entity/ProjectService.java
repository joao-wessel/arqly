package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "project_services")
public class ProjectService extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingUnit unit = BillingUnit.PROJECT;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal contractedValue = BigDecimal.ZERO;

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BillingUnit getUnit() { return unit; }
    public void setUnit(BillingUnit unit) { this.unit = unit; }
    public BigDecimal getContractedValue() { return contractedValue; }
    public void setContractedValue(BigDecimal contractedValue) { this.contractedValue = contractedValue; }
}
