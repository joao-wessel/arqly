package com.arqly.backend.repository;

import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.OriginType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.Instant;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    boolean existsByProposalIdAndDeletedFalse(UUID proposalId);
    Optional<Project> findByProposalIdAndDeletedFalse(UUID proposalId);
    Optional<Project> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<Project> findByIdAndTenantIdAndClientIdAndDeletedFalse(UUID id, UUID tenantId, UUID clientId);
    List<Project> findAllByClientIdAndTenantIdAndDeletedFalseOrderByUpdatedAtDesc(UUID clientId, UUID tenantId);
    Optional<Project> findTopByTenantIdAndCodeStartingWithOrderByCodeDesc(UUID tenantId, String prefix);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, com.arqly.backend.entity.ProjectStatus status);
    long countByTenantIdAndDeletedFalseAndCreatedAtBetween(UUID tenantId, java.time.Instant from, java.time.Instant to);
    long countByTenantIdAndTemplateIdAndDeletedFalse(UUID tenantId, UUID templateId);
    long countByTenantIdAndOriginTypeAndDeletedFalse(UUID tenantId, OriginType originType);
    @Query("select coalesce(sum(p.contractedValue), 0) from Project p where p.tenant.id = :tenantId and p.deleted = false")
    java.math.BigDecimal sumContractedValueByTenant(UUID tenantId);

    @Query("""
            select u.id, u.name, count(p)
            from Project p
            left join p.responsibleUser u
            where p.tenant.id = :tenantId
              and p.deleted = false
            group by u.id, u.name
            order by count(p) desc
            """)
    List<Object[]> countProjectsByResponsible(UUID tenantId);

    @Query("""
            select distinct p from Project p
            join fetch p.client
            left join fetch p.responsibleUser
            left join fetch p.projectManager
            left join ProjectPhase phase on phase.project = p and phase.deleted = false
            left join ProjectStage stage on stage.projectPhase = phase and stage.deleted = false
            where p.tenant.id = :tenantId and p.deleted = false
              and (:admin = true or p.responsibleUser.id = :userId or p.projectManager.id = :userId or stage.responsibleUser.id = :userId)
            order by p.updatedAt desc
            """)
    List<Project> findRelevantForHome(UUID tenantId, UUID userId, boolean admin, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "responsibleUser", "projectManager"})
    List<Project> findTop20ByTenantIdAndDeletedFalseAndUpdatedAtBeforeOrderByUpdatedAtAsc(UUID tenantId, Instant updatedAt);
}
