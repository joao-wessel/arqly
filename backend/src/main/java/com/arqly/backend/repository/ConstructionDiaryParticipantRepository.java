package com.arqly.backend.repository;
import com.arqly.backend.entity.ConstructionDiaryParticipant;
import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConstructionDiaryParticipantRepository extends JpaRepository<ConstructionDiaryParticipant, UUID> { List<ConstructionDiaryParticipant> findAllByDiaryEntryIdOrderByCreatedAtAsc(UUID entryId); void deleteByDiaryEntryId(UUID entryId); }
