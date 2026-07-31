package com.arqly.backend.repository;

import com.arqly.backend.entity.DocumentTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID>, JpaSpecificationExecutor<DocumentTemplate> {
    Optional<DocumentTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<DocumentTemplate> findFirstByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(UUID tenantId, UUID seriesId);
    List<DocumentTemplate> findAllByTenantIdAndActiveTrueAndArchivedFalseAndDeletedFalseOrderByNameAsc(UUID tenantId);
    List<DocumentTemplate> findAllByTenantIdAndSeriesIdAndDeletedFalseOrderByVersionDesc(UUID tenantId, UUID seriesId);
}
