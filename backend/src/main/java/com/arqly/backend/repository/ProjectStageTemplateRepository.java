package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStageTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageTemplateRepository extends JpaRepository<ProjectStageTemplate, UUID> {
    List<ProjectStageTemplate> findAllByProjectPhaseTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectPhaseTemplateId, UUID tenantId);
    List<ProjectStageTemplate> findAllByProjectPhaseTemplateIdAndTenantIdAndActiveTrueAndDeletedFalseOrderByOrderAsc(UUID projectPhaseTemplateId, UUID tenantId);
    Optional<ProjectStageTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByProjectPhaseTemplateProjectTemplateIdAndTenantIdAndDeletedFalse(UUID projectTemplateId, UUID tenantId);
    long countByProjectPhaseTemplateIdAndTenantIdAndDeletedFalse(UUID projectPhaseTemplateId, UUID tenantId);
}
