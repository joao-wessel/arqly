package com.arqly.backend.repository;

import com.arqly.backend.entity.TenantUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
