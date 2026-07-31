package com.arqly.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "briefings")
public class Briefing extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_template_id")
    private ProjectTemplate projectTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private TenantUser responsibleUser;

    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriefingStatus status = BriefingStatus.DRAFT;

    private BigDecimal approximateArea;
    private String workAddress;
    private String city;
    @Column(length = 2)
    private String state;
    private LocalDate desiredDeadline;
    private BigDecimal expectedBudget;

    private String architecturalStyle;
    private String colorPalette;
    @Column(columnDefinition = "text")
    private String desiredMaterials;
    @Column(columnDefinition = "text")
    private String preferenceNotes;
    @Column(columnDefinition = "text")
    private String legalRestrictions;
    @Column(columnDefinition = "text")
    private String technicalRestrictions;
    @Column(columnDefinition = "text")
    private String clientRestrictions;
    @Column(columnDefinition = "text")
    private String restrictionNotes;

    private String createdBy;
    private String updatedBy;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @OneToMany(mappedBy = "briefing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BriefingRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "briefing")
    private List<BriefingAttachment> attachments = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public ProjectTemplate getProjectTemplate() { return projectTemplate; }
    public void setProjectTemplate(ProjectTemplate projectTemplate) { this.projectTemplate = projectTemplate; }
    public TenantUser getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(TenantUser responsibleUser) { this.responsibleUser = responsibleUser; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BriefingStatus getStatus() { return status; }
    public void setStatus(BriefingStatus status) { this.status = status; }
    public BigDecimal getApproximateArea() { return approximateArea; }
    public void setApproximateArea(BigDecimal approximateArea) { this.approximateArea = approximateArea; }
    public String getWorkAddress() { return workAddress; }
    public void setWorkAddress(String workAddress) { this.workAddress = workAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public LocalDate getDesiredDeadline() { return desiredDeadline; }
    public void setDesiredDeadline(LocalDate desiredDeadline) { this.desiredDeadline = desiredDeadline; }
    public BigDecimal getExpectedBudget() { return expectedBudget; }
    public void setExpectedBudget(BigDecimal expectedBudget) { this.expectedBudget = expectedBudget; }
    public String getArchitecturalStyle() { return architecturalStyle; }
    public void setArchitecturalStyle(String architecturalStyle) { this.architecturalStyle = architecturalStyle; }
    public String getColorPalette() { return colorPalette; }
    public void setColorPalette(String colorPalette) { this.colorPalette = colorPalette; }
    public String getDesiredMaterials() { return desiredMaterials; }
    public void setDesiredMaterials(String desiredMaterials) { this.desiredMaterials = desiredMaterials; }
    public String getPreferenceNotes() { return preferenceNotes; }
    public void setPreferenceNotes(String preferenceNotes) { this.preferenceNotes = preferenceNotes; }
    public String getLegalRestrictions() { return legalRestrictions; }
    public void setLegalRestrictions(String legalRestrictions) { this.legalRestrictions = legalRestrictions; }
    public String getTechnicalRestrictions() { return technicalRestrictions; }
    public void setTechnicalRestrictions(String technicalRestrictions) { this.technicalRestrictions = technicalRestrictions; }
    public String getClientRestrictions() { return clientRestrictions; }
    public void setClientRestrictions(String clientRestrictions) { this.clientRestrictions = clientRestrictions; }
    public String getRestrictionNotes() { return restrictionNotes; }
    public void setRestrictionNotes(String restrictionNotes) { this.restrictionNotes = restrictionNotes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public List<BriefingRequirement> getRequirements() { return requirements; }
    public void setRequirements(List<BriefingRequirement> requirements) { this.requirements = requirements; }
    public List<BriefingAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<BriefingAttachment> attachments) { this.attachments = attachments; }
}
