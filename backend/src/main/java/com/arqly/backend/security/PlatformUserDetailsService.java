package com.arqly.backend.security;

import com.arqly.backend.repository.PlatformUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformUserDetailsService implements UserDetailsService {
    private final PlatformUserRepository repository;

    public PlatformUserDetailsService(PlatformUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String username) {
        var user = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado."));
        return new AuthenticatedUser(user.getId(), null, user.getEmail(), user.getPasswordHash(),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList());
    }
}
