package com.arqly.backend.repository;

import com.arqly.backend.entity.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    boolean existsByProposalIdAndDeletedFalse(UUID proposalId);
    Optional<Project> findByProposalIdAndDeletedFalse(UUID proposalId);
    Optional<Project> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<Project> findTopByTenantIdAndCodeStartingWithOrderByCodeDesc(UUID tenantId, String prefix);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, com.arqly.backend.entity.ProjectStatus status);
    long countByTenantIdAndDeletedFalseAndCreatedAtBetween(UUID tenantId, java.time.Instant from, java.time.Instant to);
    long countByTenantIdAndTemplateIdAndDeletedFalse(UUID tenantId, UUID templateId);
    @Query("select coalesce(sum(p.contractedValue), 0) from Project p where p.tenant.id = :tenantId and p.deleted = false")
    java.math.BigDecimal sumContractedValueByTenant(UUID tenantId);
}
