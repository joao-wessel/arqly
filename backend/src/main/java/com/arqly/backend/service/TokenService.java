package com.arqly.backend.service;

import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.entity.TenantUserToken;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.repository.TenantUserTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private final TenantUserTokenRepository repository;

    public TokenService(TenantUserTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String create(TenantUser user, AccessTokenType type, long hoursToExpire) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        var token = new TenantUserToken();
        token.setTenantUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(hoursToExpire * 3600));
        repository.save(token);
        return rawToken;
    }

    @Transactional
    public TenantUser consume(String rawToken, AccessTokenType type) {
        var token = repository.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new BusinessException("Token inválido."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado ou já utilizado.");
        }
        token.setUsedAt(Instant.now());
        return token.getTenantUser();
    }

    public String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }
}
