package com.arqly.backend.service;

import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.entity.PlatformUser;
import com.arqly.backend.entity.PlatformUserToken;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.entity.TenantUserToken;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.repository.PlatformUserTokenRepository;
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
    private final TenantUserTokenRepository tenantTokens;
    private final PlatformUserTokenRepository platformTokens;

    public TokenService(TenantUserTokenRepository tenantTokens, PlatformUserTokenRepository platformTokens) {
        this.tenantTokens = tenantTokens;
        this.platformTokens = platformTokens;
    }

    @Transactional
    public String create(TenantUser user, AccessTokenType type, long hoursToExpire) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        var token = new TenantUserToken();
        token.setTenantUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(hoursToExpire * 3600));
        tenantTokens.save(token);
        return rawToken;
    }

    @Transactional
    public String replace(TenantUser user, AccessTokenType type, long hoursToExpire) {
        tenantTokens.deleteByTenantUser_IdAndType(user.getId(), type);
        return create(user, type, hoursToExpire);
    }

    @Transactional
    public String replace(PlatformUser user, AccessTokenType type, long hoursToExpire) {
        platformTokens.deleteByPlatformUser_IdAndType(user.getId(), type);
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        var token = new PlatformUserToken();
        token.setPlatformUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(hoursToExpire * 3600));
        platformTokens.save(token);
        return rawToken;
    }

    @Transactional
    public TenantUser consume(String rawToken, AccessTokenType type) {
        var token = tenantTokens.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new BusinessException("Token inválido."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado ou já utilizado.");
        }
        token.setUsedAt(Instant.now());
        return token.getTenantUser();
    }

    @Transactional
    public PlatformUser consumePlatform(String rawToken, AccessTokenType type) {
        var token = platformTokens.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new BusinessException("Token inválido."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado ou já utilizado.");
        }
        token.setUsedAt(Instant.now());
        return token.getPlatformUser();
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
