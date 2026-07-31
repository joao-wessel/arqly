package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "file_versions")
public class FileVersion extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_resource_id", nullable = false)
    private FileResource file;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, unique = true, length = 700)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private long size;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private TenantUser author;

    @Column(columnDefinition = "text")
    private String revisionComment;

    public FileResource getFile() { return file; }
    public void setFile(FileResource file) { this.file = file; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public TenantUser getAuthor() { return author; }
    public void setAuthor(TenantUser author) { this.author = author; }
    public String getRevisionComment() { return revisionComment; }
    public void setRevisionComment(String revisionComment) { this.revisionComment = revisionComment; }
}
