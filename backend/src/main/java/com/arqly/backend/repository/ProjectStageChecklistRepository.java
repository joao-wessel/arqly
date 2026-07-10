package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStageChecklist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageChecklistRepository extends JpaRepository<ProjectStageChecklist, UUID> {
    List<ProjectStageChecklist> findAllByStageIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID stageId, UUID tenantId);
}
