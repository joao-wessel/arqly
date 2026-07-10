package com.arqly.backend.repository;

import com.arqly.backend.entity.ProjectService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectServiceRepository extends JpaRepository<ProjectService, UUID> {
    List<ProjectService> findAllByProjectIdOrderByCreatedAtAsc(UUID projectId);
}
