package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStageTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageTemplateRepository extends JpaRepository<ProjectStageTemplate, UUID> {
    List<ProjectStageTemplate> findAllByProjectTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectTemplateId, UUID tenantId);
    List<ProjectStageTemplate> findAllByProjectTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(UUID projectTemplateId, UUID tenantId);
    Optional<ProjectStageTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByProjectTemplateIdAndTenantIdAndDeletedFalse(UUID projectTemplateId, UUID tenantId);
}
