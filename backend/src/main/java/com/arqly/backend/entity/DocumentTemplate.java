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
@Table(name = "document_templates")
public class DocumentTemplate extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int version = 1;
    @Column(nullable = false)
    private boolean currentVersion = true;
    @Column(nullable = false)
    private UUID seriesId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private DocumentTemplate previousVersion;
    @Column(nullable = false)
    private boolean archived = false;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() { touch(); }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DocumentCategory getCategory() { return category; }
    public void setCategory(DocumentCategory category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public boolean isCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(boolean currentVersion) { this.currentVersion = currentVersion; }
    public UUID getSeriesId() { return seriesId; }
    public void setSeriesId(UUID seriesId) { this.seriesId = seriesId; }
    public DocumentTemplate getPreviousVersion() { return previousVersion; }
    public void setPreviousVersion(DocumentTemplate previousVersion) { this.previousVersion = previousVersion; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
