package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_template_id")
    private ProjectTemplate template;

    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String internalNotes;
    private String responsibleArchitect;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private TenantUser responsibleUser;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager_id")
    private TenantUser projectManager;
    private String legacyResponsibleName;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal contractedValue = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OriginType originType = OriginType.PROPOSAL;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate completedAt;
    @Column(nullable = false)
    private Instant createdFromProposalAt = Instant.now();
    private String createdBy;
    private String updatedBy;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @OneToMany(mappedBy = "project")
    private List<ProjectService> services = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public ProjectTemplate getTemplate() { return template; }
    public void setTemplate(ProjectTemplate template) { this.template = template; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public String getResponsibleArchitect() { return responsibleArchitect; }
    public void setResponsibleArchitect(String responsibleArchitect) { this.responsibleArchitect = responsibleArchitect; }
    public TenantUser getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(TenantUser responsibleUser) { this.responsibleUser = responsibleUser; }
    public TenantUser getProjectManager() { return projectManager; }
    public void setProjectManager(TenantUser projectManager) { this.projectManager = projectManager; }
    public String getLegacyResponsibleName() { return legacyResponsibleName; }
    public void setLegacyResponsibleName(String legacyResponsibleName) { this.legacyResponsibleName = legacyResponsibleName; }
    public BigDecimal getContractedValue() { return contractedValue; }
    public void setContractedValue(BigDecimal contractedValue) { this.contractedValue = contractedValue; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public OriginType getOriginType() { return originType; }
    public void setOriginType(OriginType originType) { this.originType = originType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedFromProposalAt() { return createdFromProposalAt; }
    public void setCreatedFromProposalAt(Instant createdFromProposalAt) { this.createdFromProposalAt = createdFromProposalAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public List<ProjectService> getServices() { return services; }
    public void setServices(List<ProjectService> services) { this.services = services; }
}
