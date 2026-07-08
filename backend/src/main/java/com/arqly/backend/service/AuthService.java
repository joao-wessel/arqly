package com.arqly.backend.service;

import com.arqly.backend.dto.AuthDtos.AuthResponse;
import com.arqly.backend.dto.AuthDtos.LoginRequest;
import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.repository.PlatformUserRepository;
import com.arqly.backend.repository.TenantUserRepository;
import com.arqly.backend.security.JwtService;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager platformAuthenticationManager;
    private final AuthenticationManager tenantAuthenticationManager;
    private final PlatformUserRepository platformUsers;
    private final TenantUserRepository tenantUsers;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final EmailService emailService;

    public AuthService(@Qualifier("platformAuthenticationManager") AuthenticationManager platformAuthenticationManager,
                       @Qualifier("tenantAuthenticationManager") AuthenticationManager tenantAuthenticationManager,
                       PlatformUserRepository platformUsers, TenantUserRepository tenantUsers, JwtService jwtService,
                       TokenService tokenService, PasswordEncoder passwordEncoder, SettingsService settingsService,
                       EmailService emailService) {
        this.platformAuthenticationManager = platformAuthenticationManager;
        this.tenantAuthenticationManager = tenantAuthenticationManager;
        this.platformUsers = platformUsers;
        this.tenantUsers = tenantUsers;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse platformLogin(LoginRequest request) {
        platformAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var user = platformUsers.findByEmailIgnoreCase(request.email()).orElseThrow();
        user.setLastAccessAt(Instant.now());
        var roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new AuthResponse(jwtService.createPlatformToken(user.getId(), user.getEmail(), roles), "Bearer",
                user.getId(), null, user.getName(), user.getEmail(), roles, user.getThemeMode(), user.getColorPalette());
    }

    @Transactional
    public AuthResponse tenantLogin(LoginRequest request) {
        tenantAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var user = tenantUsers.findByEmailIgnoreCase(request.email()).orElseThrow();
        user.setLastAccessAt(Instant.now());
        var roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new AuthResponse(jwtService.createTenantToken(user.getId(), user.getTenant().getId(), user.getEmail(), roles), "Bearer",
                user.getId(), user.getTenant().getId(), user.getName(), user.getEmail(), roles, user.getThemeMode(), user.getColorPalette());
    }

    @Transactional
    public void forgotPassword(String email) {
        tenantUsers.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = tokenService.create(user, AccessTokenType.PASSWORD_RESET, 2);
            String link = settingsService.getGeneral().frontendUrl() + "/reset-password?token=" + token;
            emailService.sendPasswordReset(user.getEmail(), link);
        });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        var user = tokenService.consume(token, AccessTokenType.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(password));
    }

    @Transactional
    public void firstAccess(String token, String password) {
        var user = tokenService.consume(token, AccessTokenType.FIRST_ACCESS);
        if (!user.getRoles().stream().anyMatch(role -> role.name().equals("ROLE_TENANT_ADMIN"))) {
            throw new BusinessException("Token inválido para primeiro acesso.");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
    }
}
