package com.arqly.backend.repository;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResource;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.entity.FileVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FileResourceRepository extends JpaRepository<FileResource, UUID>, JpaSpecificationExecutor<FileResource> {
    Optional<FileResource> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<FileResource> findFirstByTenantIdAndOwnerTypeAndOwnerIdAndFolderIdAndNameIgnoreCaseAndStatusNot(
            UUID tenantId, FileOwnerType ownerType, UUID ownerId, UUID folderId, String name, FileResourceStatus status);
    boolean existsByFolderIdAndStatusNot(UUID folderId, FileResourceStatus status);
    long countByTenantIdAndOwnerTypeAndOwnerIdAndStatusNot(
            UUID tenantId, FileOwnerType ownerType, UUID ownerId, FileResourceStatus status);
    List<FileResource> findAllByTenantIdAndOwnerTypeAndOwnerIdAndVisibilityAndStatusOrderByUpdatedAtDesc(
            UUID tenantId, FileOwnerType ownerType, UUID ownerId, FileVisibility visibility, FileResourceStatus status);
}
