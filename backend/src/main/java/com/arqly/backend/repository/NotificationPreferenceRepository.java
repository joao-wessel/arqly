package com.arqly.backend.repository;
import com.arqly.backend.entity.*;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,UUID>{ List<NotificationPreference> findAllByUserId(UUID userId); Optional<NotificationPreference> findByUserIdAndCategory(UUID userId,NotificationCategory category); }
