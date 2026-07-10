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
@Table(name = "proposals")
public class Proposal extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String number;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    private LocalDate validUntil;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal addition = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(columnDefinition = "text")
    private String scope;
    @Column(columnDefinition = "text")
    private String exclusions;
    @Column(columnDefinition = "text")
    private String internalNotes;
    @Column(columnDefinition = "text")
    private String clientNotes;

    private String createdBy;
    private String updatedBy;
    private Instant sentAt;
    private Instant viewedAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant expiredAt;
    private Instant cancelledAt;
    private String acceptedIp;
    private String acceptedUserAgent;
    private String rejectedIp;
    private String rejectedUserAgent;
    @Column(nullable = false)
    private boolean projectCreated = false;
    @Column(nullable = false)
    private boolean deleted = false;
    private Instant deletedAt;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalPaymentCondition> paymentConditions = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getAddition() { return addition; }
    public void setAddition(BigDecimal addition) { this.addition = addition; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getExclusions() { return exclusions; }
    public void setExclusions(String exclusions) { this.exclusions = exclusions; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public String getClientNotes() { return clientNotes; }
    public void setClientNotes(String clientNotes) { this.clientNotes = clientNotes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getViewedAt() { return viewedAt; }
    public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    public Instant getExpiredAt() { return expiredAt; }
    public void setExpiredAt(Instant expiredAt) { this.expiredAt = expiredAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getAcceptedIp() { return acceptedIp; }
    public void setAcceptedIp(String acceptedIp) { this.acceptedIp = acceptedIp; }
    public String getAcceptedUserAgent() { return acceptedUserAgent; }
    public void setAcceptedUserAgent(String acceptedUserAgent) { this.acceptedUserAgent = acceptedUserAgent; }
    public String getRejectedIp() { return rejectedIp; }
    public void setRejectedIp(String rejectedIp) { this.rejectedIp = rejectedIp; }
    public String getRejectedUserAgent() { return rejectedUserAgent; }
    public void setRejectedUserAgent(String rejectedUserAgent) { this.rejectedUserAgent = rejectedUserAgent; }
    public boolean isProjectCreated() { return projectCreated; }
    public void setProjectCreated(boolean projectCreated) { this.projectCreated = projectCreated; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public List<ProposalItem> getItems() { return items; }
    public void setItems(List<ProposalItem> items) { this.items = items; }
    public List<ProposalPaymentCondition> getPaymentConditions() { return paymentConditions; }
    public void setPaymentConditions(List<ProposalPaymentCondition> paymentConditions) { this.paymentConditions = paymentConditions; }
}
