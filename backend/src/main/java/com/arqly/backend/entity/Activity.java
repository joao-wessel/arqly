package com.arqly.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_document_id")
    private GeneratedDocument generatedDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private ProjectPhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private ProjectStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private TenantUser author;

    @Column(nullable = false)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityVisibility visibility = ActivityVisibility.INTERNAL;

    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @OneToOne(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ActivityContent content;

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public GeneratedDocument getGeneratedDocument() { return generatedDocument; }
    public void setGeneratedDocument(GeneratedDocument generatedDocument) { this.generatedDocument = generatedDocument; }
    public ProjectPhase getPhase() { return phase; }
    public void setPhase(ProjectPhase phase) { this.phase = phase; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public TenantUser getAuthor() { return author; }
    public void setAuthor(TenantUser author) { this.author = author; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public ActivityVisibility getVisibility() { return visibility; }
    public void setVisibility(ActivityVisibility visibility) { this.visibility = visibility; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public ActivityContent getContent() { return content; }
    public void setContent(ActivityContent content) {
        this.content = content;
        if (content != null) content.setActivity(this);
    }
}
