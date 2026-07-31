package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "file_resources")
public class FileResource extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FileOwnerType ownerType;

    @Column(nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, length = 30)
    private String extension;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, unique = true, length = 700)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileVisibility visibility = FileVisibility.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileResourceStatus status = FileResourceStatus.ACTIVE;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private TenantUser uploadedBy;

    private Instant deletedAt;

    @ManyToMany
    @JoinTable(name = "file_resource_tags",
            joinColumns = @JoinColumn(name = "file_resource_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<FileTag> tags = new LinkedHashSet<>();

    @PreUpdate
    void onUpdate() { touch(); }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public FileOwnerType getOwnerType() { return ownerType; }
    public void setOwnerType(FileOwnerType ownerType) { this.ownerType = ownerType; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public FileVisibility getVisibility() { return visibility; }
    public void setVisibility(FileVisibility visibility) { this.visibility = visibility; }
    public FileResourceStatus getStatus() { return status; }
    public void setStatus(FileResourceStatus status) { this.status = status; }
    public TenantUser getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(TenantUser uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Set<FileTag> getTags() { return tags; }
    public void setTags(Set<FileTag> tags) { this.tags = tags; }
}
