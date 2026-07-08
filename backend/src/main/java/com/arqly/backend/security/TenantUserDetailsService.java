package com.arqly.backend.security;

import com.arqly.backend.repository.TenantUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUserDetailsService implements UserDetailsService {
    private final TenantUserRepository repository;

    public TenantUserDetailsService(TenantUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String username) {
        var user = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return new AuthenticatedUser(user.getId(), user.getTenant().getId(), user.getEmail(), user.getPasswordHash(),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList());
    }
}
