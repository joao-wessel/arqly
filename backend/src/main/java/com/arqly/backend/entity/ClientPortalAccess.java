package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_portal_accesses")
public class ClientPortalAccess extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @Column(nullable = false, unique = true)
    private UUID token;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked = false;
    private Instant lastAccessAt;
    @Column(nullable = false)
    private boolean active = true;

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public UUID getToken() { return token; }
    public void setToken(UUID token) { this.token = token; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public Instant getLastAccessAt() { return lastAccessAt; }
    public void setLastAccessAt(Instant lastAccessAt) { this.lastAccessAt = lastAccessAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
