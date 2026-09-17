package com.arqly.backend.repository;
import com.arqly.backend.entity.ConstructionDiaryObservation;
import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConstructionDiaryObservationRepository extends JpaRepository<ConstructionDiaryObservation, UUID> { List<ConstructionDiaryObservation> findAllByDiaryEntryIdOrderByOrderAsc(UUID entryId); void deleteByDiaryEntryId(UUID entryId); }
