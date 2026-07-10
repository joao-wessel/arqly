package com.arqly.backend.repository;

import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.entity.TenantUserToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserTokenRepository extends JpaRepository<TenantUserToken, UUID> {
    Optional<TenantUserToken> findByTokenHashAndType(String tokenHash, AccessTokenType type);
    void deleteByTenantUser_IdAndType(UUID tenantUserId, AccessTokenType type);
}
