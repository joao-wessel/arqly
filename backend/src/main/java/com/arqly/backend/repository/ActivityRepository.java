package com.arqly.backend.repository;

import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {
    Optional<Activity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<Activity> findTop20ByTenantIdAndClientIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(
            UUID tenantId, UUID clientId, ActivityVisibility visibility);
    List<Activity> findTop20ByTenantIdAndProjectIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, ActivityVisibility visibility);
}
