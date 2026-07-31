package com.arqly.backend.repository;

import com.arqly.backend.entity.Briefing;
import com.arqly.backend.entity.BriefingStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BriefingRepository extends JpaRepository<Briefing, UUID>, JpaSpecificationExecutor<Briefing> {
    Optional<Briefing> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, BriefingStatus status);
}
