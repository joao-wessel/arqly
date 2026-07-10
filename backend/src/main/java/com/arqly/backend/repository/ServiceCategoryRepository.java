package com.arqly.backend.repository;

import com.arqly.backend.entity.ServiceCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID>, JpaSpecificationExecutor<ServiceCategory> {
    Optional<ServiceCategory> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<ServiceCategory> findAllByTenantIdAndDeletedFalseOrderByNameAsc(UUID tenantId);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
