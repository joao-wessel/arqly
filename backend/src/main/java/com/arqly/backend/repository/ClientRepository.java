package com.arqly.backend.repository;

import com.arqly.backend.entity.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
}
