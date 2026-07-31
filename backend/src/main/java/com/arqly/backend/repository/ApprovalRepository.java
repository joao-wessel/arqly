package com.arqly.backend.repository;

import com.arqly.backend.entity.Approval;
import com.arqly.backend.entity.ApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    Optional<Approval> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<Approval> findAllByProjectIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID projectId, UUID tenantId);
    List<Approval> findAllByClientIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID clientId, UUID tenantId);
    long countByClientIdAndTenantIdAndStatusAndDeletedFalse(UUID clientId, UUID tenantId, ApprovalStatus status);
}
