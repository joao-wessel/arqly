package com.arqly.backend.service.file;

import com.arqly.backend.exception.BusinessException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalStorageProvider implements StorageProvider {
    private final Path root;

    public LocalStorageProvider(@Value("${arqly.storage.local-root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar o armazenamento local.", exception);
        }
    }

    @Override
    public String provider() { return "LOCAL"; }

    @Override
    public StoredFile store(UUID tenantId, String extension, InputStream input) {
        var safeExtension = extension == null || extension.isBlank() ? "bin" : extension.replaceAll("[^a-zA-Z0-9]", "");
        var key = tenantId + "/" + UUID.randomUUID() + "." + safeExtension.toLowerCase();
        var target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            var digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (var source = new DigestInputStream(new BufferedInputStream(input), digest);
                 var output = new BufferedOutputStream(Files.newOutputStream(target))) {
                size = source.transferTo(output);
            }
            return new StoredFile(key, HexFormat.of().formatHex(digest.digest()), size);
        } catch (IOException | NoSuchAlgorithmException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) {}
            throw new BusinessException("Não foi possível armazenar o arquivo.");
        }
    }

    @Override
    public InputStream load(String storageKey) {
        try {
            return Files.newInputStream(resolve(storageKey));
        } catch (IOException exception) {
            throw new BusinessException("O conteúdo do arquivo não está disponível.");
        }
    }

    private Path resolve(String storageKey) {
        var resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) throw new BusinessException("Chave de armazenamento inválida.");
        return resolved;
    }
}
