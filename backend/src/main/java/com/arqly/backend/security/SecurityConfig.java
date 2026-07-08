package com.arqly.backend.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain platformSecurity(HttpSecurity http, JwtService jwtService) throws Exception {
        return http.securityMatcher("/api/platform/**", "/api/auth/platform/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/platform/login").permitAll()
                        .anyRequest().hasRole("PLATFORM_ADMIN"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService::parsePlatform), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain tenantSecurity(HttpSecurity http, JwtService jwtService) throws Exception {
        return http.securityMatcher("/api/tenant/**", "/api/auth/tenant/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/tenant/login", "/api/auth/tenant/forgot-password", "/api/auth/tenant/reset-password", "/api/auth/tenant/first-access").permitAll()
                        .anyRequest().hasAnyRole("TENANT_ADMIN", "USER"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService::parseTenant), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicSecurity(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    @Primary
    AuthenticationManager platformAuthenticationManager(@Qualifier("platformUserDetailsService") UserDetailsService platformUserDetailsService,
                                                        PasswordEncoder encoder) {
        return new ProviderManager(authenticationProvider(platformUserDetailsService, encoder));
    }

    @Bean
    AuthenticationManager tenantAuthenticationManager(@Qualifier("tenantUserDetailsService") UserDetailsService tenantUserDetailsService,
                                                      PasswordEncoder encoder) {
        return new ProviderManager(authenticationProvider(tenantUserDetailsService, encoder));
    }

    private DaoAuthenticationProvider authenticationProvider(UserDetailsService service, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(service);
        provider.setPasswordEncoder(encoder);
        return provider;
    }
}
