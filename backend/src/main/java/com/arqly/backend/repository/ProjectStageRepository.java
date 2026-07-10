package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.ProjectStageStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, UUID> {
    List<ProjectStage> findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectId, UUID tenantId);
    Optional<ProjectStage> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProjectStageStatus status);
    long countByTenantIdAndStatusNotAndPlannedEndBeforeAndDeletedFalse(UUID tenantId, ProjectStageStatus status, LocalDate date);

}
