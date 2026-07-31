package com.arqly.backend.service.file;

import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.TenantRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantFileOwnerResolver implements FileOwnerResolver {
    private final TenantRepository repository;
    public TenantFileOwnerResolver(TenantRepository repository) { this.repository = repository; }
    public FileOwnerType type() { return FileOwnerType.TENANT; }
    public FileOwnerContext resolve(UUID tenantId, UUID ownerId) {
        if (!tenantId.equals(ownerId)) throw new NotFoundException("Escritório não encontrado.");
        var value = repository.findById(tenantId).orElseThrow(() -> new NotFoundException("Escritório não encontrado."));
        return new FileOwnerContext(ownerId, value.getTradeName(), null, null, null, null, null);
    }
}
