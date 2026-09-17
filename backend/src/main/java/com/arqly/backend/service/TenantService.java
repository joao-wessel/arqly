package com.arqly.backend.service;

import com.arqly.backend.dto.TenantDtos.CreateTenantAdminRequest;
import com.arqly.backend.dto.TenantDtos.TenantAdminResponse;
import com.arqly.backend.dto.TenantDtos.TenantRequest;
import com.arqly.backend.dto.TenantDtos.TenantResponse;
import com.arqly.backend.entity.AccessTokenType;
import com.arqly.backend.entity.Role;
import com.arqly.backend.entity.TenantUser;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.TenantMapper;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository userRepository;
    private final TenantMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SettingsService settingsService;
    private final EmailService emailService;
    private final ResidentialProjectTemplateService residentialTemplateService;

    public TenantService(TenantRepository tenantRepository, TenantUserRepository userRepository, TenantMapper mapper,
                         PasswordEncoder passwordEncoder, TokenService tokenService, SettingsService settingsService,
                         EmailService emailService, ResidentialProjectTemplateService residentialTemplateService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.settingsService = settingsService;
        this.emailService = emailService;
        this.residentialTemplateService = residentialTemplateService;
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        if (tenantRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessException("CPF/CNPJ já cadastrado.");
        }
        var tenant = mapper.toEntity(request);
        if (tenant.getStatus() == null) {
            tenant.setStatus(com.arqly.backend.entity.TenantStatus.ACTIVE);
        }
        tenant = tenantRepository.save(tenant);
        residentialTemplateService.ensureDefaultTemplate(tenant);
        return mapper.toResponse(tenant);
    }

    public Page<TenantResponse> list(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(mapper::toResponse);
    }

    public TenantResponse get(UUID id) {
        return mapper.toResponse(tenantRepository.findById(id).orElseThrow(() -> new NotFoundException("Tenant não encontrado.")));
    }

    @Transactional
    public TenantResponse update(UUID id, TenantRequest request) {
        var tenant = tenantRepository.findById(id).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        mapper.update(request, tenant);
        return mapper.toResponse(tenant);
    }

    @Transactional
    public void delete(UUID id) {
        if (!tenantRepository.existsById(id)) {
            throw new NotFoundException("Tenant não encontrado.");
        }
        if (userRepository.countByTenantId(id) > 0) {
            throw new BusinessException("Remova os usuários do tenant antes de excluí-lo.");
        }
        tenantRepository.deleteById(id);
    }

    @Transactional
    public TenantAdminResponse createAdmin(UUID tenantId, CreateTenantAdminRequest request) {
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var user = userRepository.findByEmailIgnoreCaseAndTenantId(request.email(), tenantId)
                .orElseGet(() -> {
                    var newUser = new TenantUser();
                    newUser.setTenant(tenant);
                    newUser.setEmail(request.email());
                    newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return newUser;
                });

        user.setName(request.name());
        user.setActive(true);
        user.getRoles().add(Role.ROLE_TENANT_ADMIN);
        user = userRepository.save(user);

        String token = tokenService.replace(user, AccessTokenType.FIRST_ACCESS, 1);
        String link = settingsService.getGeneral().frontendUrl() + "/first-access?token=" + token;
        emailService.sendFirstAccess(user.getEmail(), link);
        return new TenantAdminResponse(user.getId(), tenant.getId(), user.getName(), user.getEmail(), link);
    }
}
