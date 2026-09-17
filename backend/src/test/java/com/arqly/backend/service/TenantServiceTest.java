package com.arqly.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.dto.TenantDtos.TenantRequest;
import com.arqly.backend.entity.PersonType;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.mapper.TenantMapper;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {
    @Mock private TenantRepository tenants;
    @Mock private TenantUserRepository users;
    @Mock private TenantMapper mapper;
    @Mock private PasswordEncoder encoder;
    @Mock private TokenService tokens;
    @Mock private SettingsService settings;
    @Mock private EmailService email;
    @Mock private ResidentialProjectTemplateService templates;

    @Test
    void createsTheResidentialTemplateAsPartOfTenantCreation() {
        Tenant tenant = new Tenant();
        TenantRequest request = new TenantRequest("Piloto", "Piloto", PersonType.LEGAL_ENTITY, "12345678000199", "piloto@test.local", null, null, null, null, null, null);
        when(tenants.existsByCnpj(request.cnpj())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(tenant);
        when(tenants.save(tenant)).thenReturn(tenant);
        TenantService service = new TenantService(tenants, users, mapper, encoder, tokens, settings, email, templates);

        service.create(request);

        verify(templates).ensureDefaultTemplate(tenant);
        verify(mapper).toResponse(tenant);
    }
}
