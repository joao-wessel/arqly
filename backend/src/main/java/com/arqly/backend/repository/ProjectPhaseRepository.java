package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectPhase;
import com.arqly.backend.entity.ProjectStageStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, UUID> {
    List<ProjectPhase> findAllByProjectIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectId, UUID tenantId);
    Optional<ProjectPhase> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProjectStageStatus status);
}
