package com.arqly.backend.repository;

import com.arqly.backend.entity.TenantUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmailIgnoreCase(String email);
    Optional<TenantUser> findByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
    Page<TenantUser> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<TenantUser> findAllByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
    Optional<TenantUser> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
