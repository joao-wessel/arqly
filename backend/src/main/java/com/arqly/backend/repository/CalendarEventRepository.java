package com.arqly.backend.repository;

import com.arqly.backend.entity.CalendarEvent;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    Optional<CalendarEvent> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    @Query("select e from CalendarEvent e where e.tenant.id=:tenantId and e.deleted=false and e.startDateTime < :end and (e.endDateTime is null or e.endDateTime >= :start) order by e.startDateTime")
    List<CalendarEvent> findForInterval(@Param("tenantId") UUID tenantId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
