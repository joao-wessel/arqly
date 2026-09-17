package com.arqly.backend.repository;

import com.arqly.backend.entity.ConstructionDiaryEntry;
import com.arqly.backend.entity.ConstructionDiaryStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConstructionDiaryEntryRepository extends JpaRepository<ConstructionDiaryEntry, UUID>, JpaSpecificationExecutor<ConstructionDiaryEntry> {
    Optional<ConstructionDiaryEntry> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<ConstructionDiaryEntry> findAllByProjectIdAndTenantIdAndDeletedFalseOrderByEntryDateDescCreatedAtDesc(UUID projectId, UUID tenantId);
    List<ConstructionDiaryEntry> findAllByProjectIdAndTenantIdAndStatusAndVisibilityAndDeletedFalseOrderByEntryDateDescCreatedAtDesc(UUID projectId, UUID tenantId, ConstructionDiaryStatus status, com.arqly.backend.entity.ActivityVisibility visibility);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ConstructionDiaryStatus status);
    Optional<ConstructionDiaryEntry> findTopByTenantIdAndNextVisitDateIsNotNullAndDeletedFalseOrderByNextVisitDateDesc(UUID tenantId);
    Optional<ConstructionDiaryEntry> findTopByTenantIdAndDeletedFalseOrderByEntryDateDesc(UUID tenantId);
    long countByTenantIdAndEntryDateAfterAndDeletedFalse(UUID tenantId, LocalDate date);
    List<ConstructionDiaryEntry> findAllByTenantIdAndDeletedFalseAndNextVisitDateBetween(UUID tenantId, LocalDate start, LocalDate end);
}
