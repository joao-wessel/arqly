package com.arqly.backend.repository;

import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.entity.PlatformUserToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformUserTokenRepository extends JpaRepository<PlatformUserToken, UUID> {
    Optional<PlatformUserToken> findByTokenHashAndType(String tokenHash, AccessTokenType type);
    void deleteByPlatformUser_IdAndType(UUID platformUserId, AccessTokenType type);
}
