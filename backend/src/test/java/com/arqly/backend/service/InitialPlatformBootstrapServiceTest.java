package com.arqly.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.arqly.backend.config.InitialBootstrapProperties;
import com.arqly.backend.entity.PlatformUser;
import com.arqly.backend.entity.Role;
import com.arqly.backend.repository.PlatformUserRepository;
import com.arqly.backend.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialPlatformBootstrapServiceTest {
    private final InitialBootstrapProperties enabled = new InitialBootstrapProperties(true, "Administradora", "admin@piloto.test", "senha-segura");
    @Mock private PlatformUserRepository users;
    @Mock private TenantRepository tenants;
    @Mock private PasswordEncoder encoder;

    @Test
    void createsOnlyTheInitialPlatformAdminWhenEnabled() {
        when(users.existsByEmailIgnoreCase(enabled.adminEmail())).thenReturn(false);
        when(encoder.encode(enabled.adminPassword())).thenReturn("encoded");
        new InitialPlatformBootstrapService(users, encoder).bootstrap(enabled);
        var captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getRoles()).containsExactly(Role.ROLE_PLATFORM_ADMIN);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        verifyNoInteractions(tenants);
    }

    @Test
    void doesNothingWhenDisabled() {
        new InitialPlatformBootstrapService(users, encoder).bootstrap(new InitialBootstrapProperties(false, "", "", ""));
        verifyNoInteractions(users, encoder, tenants);
    }

    @Test
    void doesNotDuplicateAnExistingAdmin() {
        when(users.existsByEmailIgnoreCase(enabled.adminEmail())).thenReturn(true);
        new InitialPlatformBootstrapService(users, encoder).bootstrap(enabled);
        verify(users, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void storesThePasswordUsingBCrypt() {
        when(users.existsByEmailIgnoreCase(enabled.adminEmail())).thenReturn(false);
        new InitialPlatformBootstrapService(users, new BCryptPasswordEncoder()).bootstrap(enabled);
        var captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(users).save(captor.capture());
        assertThat(new BCryptPasswordEncoder().matches(enabled.adminPassword(), captor.getValue().getPasswordHash())).isTrue();
    }
}
