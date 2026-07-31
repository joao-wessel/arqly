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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_documents")
public class GeneratedDocument extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(nullable = false)
    private int version = 1;
    @Column(nullable = false)
    private boolean currentVersion = true;
    @Column(nullable = false)
    private UUID seriesId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private GeneratedDocument previousVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneratedDocumentStatus status = GeneratedDocumentStatus.GENERATED;
    @Column(nullable = false)
    private boolean clientVisible = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id")
    private TenantUser generatedBy;
    @Column(nullable = false)
    private String generatedByName;
    @Column(nullable = false)
    private Instant generatedAt = Instant.now();
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() { touch(); }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public DocumentTemplate getTemplate() { return template; }
    public void setTemplate(DocumentTemplate template) { this.template = template; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public boolean isCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(boolean currentVersion) { this.currentVersion = currentVersion; }
    public UUID getSeriesId() { return seriesId; }
    public void setSeriesId(UUID seriesId) { this.seriesId = seriesId; }
    public GeneratedDocument getPreviousVersion() { return previousVersion; }
    public void setPreviousVersion(GeneratedDocument previousVersion) { this.previousVersion = previousVersion; }
    public GeneratedDocumentStatus getStatus() { return status; }
    public void setStatus(GeneratedDocumentStatus status) { this.status = status; }
    public boolean isClientVisible() { return clientVisible; }
    public void setClientVisible(boolean clientVisible) { this.clientVisible = clientVisible; }
    public TenantUser getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(TenantUser generatedBy) { this.generatedBy = generatedBy; }
    public String getGeneratedByName() { return generatedByName; }
    public void setGeneratedByName(String generatedByName) { this.generatedByName = generatedByName; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
