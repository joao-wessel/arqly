package com.arqly.backend.service;

import com.arqly.backend.service.file.StorageProvider;
import com.arqly.backend.service.file.StoredFile;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {
    private final StorageProvider provider;

    public FileStorageService(List<StorageProvider> providers) {
        this.provider = providers.stream().filter(item -> "LOCAL".equals(item.provider())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Storage local não configurado."));
    }

    public StoredFile store(UUID tenantId, String extension, InputStream input) {
        return provider.store(tenantId, extension, input);
    }

    public InputStream load(String storageKey) {
        return provider.load(storageKey);
    }
}
