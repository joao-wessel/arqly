package com.arqly.backend.repository;

import com.arqly.backend.entity.Tenant;
import com.arqly.backend.entity.TenantStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    long countByStatus(TenantStatus status);
    boolean existsByCnpj(String cnpj);
    Optional<Tenant> findByCnpj(String cnpj);
}
