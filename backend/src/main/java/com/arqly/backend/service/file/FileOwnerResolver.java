package com.arqly.backend.service.file;

import com.arqly.backend.entity.FileOwnerType;
import java.util.UUID;

public interface FileOwnerResolver {
    FileOwnerType type();
    FileOwnerContext resolve(UUID tenantId, UUID ownerId);
}
