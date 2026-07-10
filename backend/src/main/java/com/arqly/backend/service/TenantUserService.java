package com.arqly.backend.service;

import com.arqly.backend.dto.TenantUserDtos.TenantUserRequest;
import com.arqly.backend.dto.TenantUserDtos.TenantUserResponse;
import com.arqly.backend.entity.Role;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUserService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public TenantUserService(TenantRepository tenantRepository, TenantUserRepository repository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<TenantUserResponse> list(UUID tenantId, Pageable pageable) {
        return repository.findAllByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional
    public TenantUserResponse create(UUID tenantId, TenantUserRequest request) {
        if (repository.existsByEmailIgnoreCaseAndTenantId(request.email(), tenantId)) {
            throw new BusinessException("E-mail já cadastrado neste escritório.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Senha é obrigatória.");
        }
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var user = new TenantUser();
        user.setTenant(tenant);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setActive(request.active());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(roles(request.tenantAdmin()));
        return toResponse(repository.save(user));
    }

    @Transactional
    public TenantUserResponse update(UUID tenantId, UUID id, TenantUserRequest request) {
        var user = repository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        repository.findByEmailIgnoreCaseAndTenantId(request.email(), tenantId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("E-mail já cadastrado neste escritório.");
                });
        ensureAnotherAdminExistsWhenNeeded(tenantId, id, request);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setActive(request.active());
        user.setRoles(roles(request.tenantAdmin()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("Você não pode excluir seu próprio usuário.");
        }
        var user = repository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (user.getRoles().contains(Role.ROLE_TENANT_ADMIN) && activeAdminCountExcluding(tenantId, id) == 0) {
            throw new BusinessException("Pelo menos um administrador do escritório deve permanecer.");
        }
        repository.delete(user);
    }

    private void ensureAnotherAdminExistsWhenNeeded(UUID tenantId, UUID userId, TenantUserRequest request) {
        if (!request.active() || !request.tenantAdmin()) {
            var current = repository.findByIdAndTenantId(userId, tenantId).orElseThrow();
            if (current.getRoles().contains(Role.ROLE_TENANT_ADMIN) && activeAdminCountExcluding(tenantId, userId) == 0) {
                throw new BusinessException("Pelo menos um administrador do escritório deve permanecer.");
            }
        }
    }

    private long activeAdminCountExcluding(UUID tenantId, UUID ignoredUserId) {
        return repository.findAllByTenantId(tenantId, Pageable.unpaged()).stream()
                .filter(user -> !user.getId().equals(ignoredUserId))
                .filter(TenantUser::isActive)
                .filter(user -> user.getRoles().contains(Role.ROLE_TENANT_ADMIN))
                .count();
    }

    private Set<Role> roles(boolean tenantAdmin) {
        var roles = new HashSet<>(Set.of(Role.ROLE_USER));
        if (tenantAdmin) {
            roles.add(Role.ROLE_TENANT_ADMIN);
        }
        return roles;
    }

    private TenantUserResponse toResponse(TenantUser user) {
        boolean tenantAdmin = user.getRoles().contains(Role.ROLE_TENANT_ADMIN);
        return new TenantUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                tenantAdmin,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getLastAccessAt(),
                user.getCreatedAt()
        );
    }
}
