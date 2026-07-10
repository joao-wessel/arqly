package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStageTemplateChecklist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageTemplateChecklistRepository extends JpaRepository<ProjectStageTemplateChecklist, UUID> {
    List<ProjectStageTemplateChecklist> findAllByStageTemplateIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID stageTemplateId, UUID tenantId);
}
