package com.arqly.backend.repository;
import com.arqly.backend.entity.ConstructionDiaryDecision;
import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConstructionDiaryDecisionRepository extends JpaRepository<ConstructionDiaryDecision, UUID> { List<ConstructionDiaryDecision> findAllByDiaryEntryIdOrderByDecisionDateAsc(UUID entryId); void deleteByDiaryEntryId(UUID entryId); }
