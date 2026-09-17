package com.arqly.backend.repository;
import com.arqly.backend.entity.ConstructionDiaryPhoto;
import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConstructionDiaryPhotoRepository extends JpaRepository<ConstructionDiaryPhoto, UUID> {
    List<ConstructionDiaryPhoto> findAllByDiaryEntryIdOrderByOrderAsc(UUID entryId);
}
