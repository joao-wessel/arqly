package com.arqly.backend.service;

import com.arqly.backend.dto.PlatformUserDtos.PlatformUserRequest;
import com.arqly.backend.dto.PlatformUserDtos.PlatformUserResponse;
import com.arqly.backend.entity.PlatformUser;
import com.arqly.backend.entity.Role;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.PlatformUserRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformUserService {
    private final PlatformUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PlatformUserService(PlatformUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<PlatformUserResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public PlatformUserResponse create(PlatformUserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Senha é obrigatória.");
        }
        var user = new PlatformUser();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setActive(request.active());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.ROLE_PLATFORM_ADMIN));
        return toResponse(repository.save(user));
    }

    @Transactional
    public PlatformUserResponse update(UUID id, PlatformUserRequest request) {
        var user = repository.findById(id).orElseThrow(() -> new NotFoundException("Administrador não encontrado."));
        repository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("E-mail já cadastrado.");
                });
        user.setName(request.name());
        user.setEmail(request.email());
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Administrador não encontrado.");
        }
        if (repository.count() <= 1) {
            throw new BusinessException("Pelo menos um administrador da plataforma deve permanecer.");
        }
        repository.deleteById(id);
    }

    private PlatformUserResponse toResponse(PlatformUser user) {
        return new PlatformUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getLastAccessAt(),
                user.getCreatedAt()
        );
    }
}
