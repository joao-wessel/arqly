package com.arqly.backend.repository;

import com.arqly.backend.entity.Service;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceRepository extends JpaRepository<Service, UUID>, JpaSpecificationExecutor<Service> {
    Optional<Service> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
    long countByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId);
    long countByTenantIdAndActiveFalseAndDeletedFalse(UUID tenantId);
    long countByCategoryIdAndDeletedFalse(UUID categoryId);
}
