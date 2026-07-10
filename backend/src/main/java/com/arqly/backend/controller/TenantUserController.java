package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.TenantUserDtos.TenantUserRequest;
import com.arqly.backend.dto.TenantUserDtos.TenantUserResponse;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.TenantUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/users")
public class TenantUserController {
    private final TenantUserService service;

    public TenantUserController(TenantUserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<TenantUserResponse>> list(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), pageable));
    }

    @PostMapping
    public ApiResponse<TenantUserResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody TenantUserRequest request) {
        return ApiResponse.ok(service.create(user.getTenantId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantUserResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody TenantUserRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.getTenantId(), id, user.getId());
        return ApiResponse.message("Usuário excluído.");
    }
}
