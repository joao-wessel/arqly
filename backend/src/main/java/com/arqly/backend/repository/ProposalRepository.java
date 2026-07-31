package com.arqly.backend.repository;

import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.OriginType;
import com.arqly.backend.entity.ProposalStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends JpaRepository<Proposal, UUID>, JpaSpecificationExecutor<Proposal> {
    Optional<Proposal> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<Proposal> findTopByTenantIdAndNumberStartingWithOrderByNumberDesc(UUID tenantId, String prefix);
    List<Proposal> findAllByClientIdAndDeletedFalseAndStatusInOrderByCreatedAtDesc(UUID clientId, Collection<ProposalStatus> statuses);
    Optional<Proposal> findByBriefingIdAndTenantIdAndDeletedFalse(UUID briefingId, UUID tenantId);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProposalStatus status);
    long countByTenantIdAndBriefingIsNotNullAndDeletedFalse(UUID tenantId);
    long countByTenantIdAndOriginTypeAndDeletedFalse(UUID tenantId, OriginType originType);

    @Query("select coalesce(sum(p.total), 0) from Proposal p where p.tenant.id = :tenantId and p.deleted = false")
    BigDecimal sumTotalByTenant(@Param("tenantId") UUID tenantId);
}
