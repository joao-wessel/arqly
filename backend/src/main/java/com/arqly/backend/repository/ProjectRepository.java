package com.arqly.backend.repository;

import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.OriginType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

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
}
