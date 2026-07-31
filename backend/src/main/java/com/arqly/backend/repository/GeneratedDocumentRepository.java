package com.arqly.backend.repository;

import com.arqly.backend.entity.GeneratedDocument;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID>, JpaSpecificationExecutor<GeneratedDocument> {
    Optional<GeneratedDocument> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<GeneratedDocument> findByIdAndTenantIdAndClientIdAndClientVisibleTrueAndDeletedFalse(UUID id, UUID tenantId, UUID clientId);
    Optional<GeneratedDocument> findFirstByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(UUID tenantId, UUID seriesId);
    List<GeneratedDocument> findAllByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(UUID tenantId, UUID seriesId);
    List<GeneratedDocument> findAllByClientIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalseOrderByGeneratedAtDesc(UUID clientId, UUID tenantId);
    List<GeneratedDocument> findAllByProjectIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalseOrderByGeneratedAtDesc(UUID projectId, UUID tenantId);
    long countByClientIdAndTenantIdAndClientVisibleTrueAndCurrentVersionTrueAndDeletedFalse(UUID clientId, UUID tenantId);
}
