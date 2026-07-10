package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, UUID> {
    Page<ProjectTemplate> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    List<ProjectTemplate> findAllByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(UUID tenantId);
    Optional<ProjectTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    boolean existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(UUID tenantId, String name);
}
