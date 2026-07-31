package com.arqly.backend.service.file;

import java.io.InputStream;
import java.util.UUID;

public interface StorageProvider {
    String provider();
    StoredFile store(UUID tenantId, String extension, InputStream input);
    InputStream load(String storageKey);
}
