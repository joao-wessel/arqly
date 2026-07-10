package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.AuthDtos.AuthResponse;
import com.arqly.backend.dto.AuthDtos.FirstAccessRequest;
import com.arqly.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.arqly.backend.dto.AuthDtos.LoginRequest;
import com.arqly.backend.dto.AuthDtos.ResetPasswordRequest;
import com.arqly.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/tenant")
public class TenantAuthController {
    private final AuthService authService;

    public TenantAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.tenantLogin(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.message("Se o e-mail existir, enviaremos as instruções.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ApiResponse.message("Senha redefinida com sucesso.");
    }

    @PostMapping("/first-access")
    public ApiResponse<Void> firstAccess(@Valid @RequestBody FirstAccessRequest request) {
        authService.firstAccess(request.token(), request.password(), request.name());
        return ApiResponse.message("Senha definida com sucesso.");
    }
}
