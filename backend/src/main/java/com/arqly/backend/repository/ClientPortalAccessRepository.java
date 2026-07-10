package com.arqly.backend.repository;

import com.arqly.backend.entity.ClientPortalAccess;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientPortalAccessRepository extends JpaRepository<ClientPortalAccess, UUID> {
    Optional<ClientPortalAccess> findByClientIdAndActiveTrueAndRevokedFalse(UUID clientId);
    List<ClientPortalAccess> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);
    Optional<ClientPortalAccess> findByToken(UUID token);
}
