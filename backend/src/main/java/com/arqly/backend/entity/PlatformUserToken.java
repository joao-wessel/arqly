package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_user_tokens")
public class PlatformUserToken extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_user_id", nullable = false)
    private PlatformUser platformUser;
    @Column(nullable = false, unique = true)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessTokenType type;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant usedAt;

    public PlatformUser getPlatformUser() { return platformUser; }
    public void setPlatformUser(PlatformUser platformUser) { this.platformUser = platformUser; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public AccessTokenType getType() { return type; }
    public void setType(AccessTokenType type) { this.type = type; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
}
