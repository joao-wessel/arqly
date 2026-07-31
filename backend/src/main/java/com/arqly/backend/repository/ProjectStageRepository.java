package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.ProjectStageStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, UUID> {
    List<ProjectStage> findAllByProjectPhaseIdAndTenantIdAndDeletedFalseOrderByOrderAsc(UUID projectPhaseId, UUID tenantId);
    @Query("""
            select stage
            from ProjectStage stage
            where stage.projectPhase.project.id = :projectId
              and stage.tenant.id = :tenantId
              and stage.deleted = false
            order by stage.projectPhase.order asc, stage.order asc
            """)
    List<ProjectStage> findAllByProjectIdOrdered(@Param("projectId") UUID projectId, @Param("tenantId") UUID tenantId);
    Optional<ProjectStage> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProjectStageStatus status);
    long countByTenantIdAndStatusNotAndPlannedEndBeforeAndDeletedFalse(UUID tenantId, ProjectStageStatus status, LocalDate date);

    @Query("""
            select coalesce(stageUser.id, projectUser.id), coalesce(stageUser.name, projectUser.name), count(stage)
            from ProjectStage stage
            left join stage.responsibleUser stageUser
            left join stage.projectPhase phase
            left join phase.project project
            left join project.responsibleUser projectUser
            where stage.tenant.id = :tenantId
              and stage.deleted = false
            group by coalesce(stageUser.id, projectUser.id), coalesce(stageUser.name, projectUser.name)
            order by count(stage) desc
            """)
    List<Object[]> countStagesByResponsible(@Param("tenantId") UUID tenantId);

}
