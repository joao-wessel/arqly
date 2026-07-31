package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectPhaseTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectPhaseTemplateRepository extends JpaRepository<ProjectPhaseTemplate, UUID> {
    List<ProjectPhaseTemplate> findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectTemplateId, UUID tenantId);
    List<ProjectPhaseTemplate> findAllByProjectTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(UUID projectTemplateId, UUID tenantId);
    Optional<ProjectPhaseTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByProjectTemplateIdAndTenantIdAndDeletedFalse(UUID projectTemplateId, UUID tenantId);
}
