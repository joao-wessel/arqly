package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.TenantDtos.CreateTenantAdminRequest;
import com.arqly.backend.dto.TenantDtos.TenantAdminResponse;
import com.arqly.backend.dto.TenantDtos.TenantRequest;
import com.arqly.backend.dto.TenantDtos.TenantResponse;
import com.arqly.backend.service.TenantService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/tenants")
public class TenantAdminController {
    private final TenantService tenantService;

    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ApiResponse<Page<TenantResponse>> list(Pageable pageable) {
        return ApiResponse.ok(tenantService.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.get(id));
    }

    @PostMapping
    public ApiResponse<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        return ApiResponse.ok(tenantService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantResponse> update(@PathVariable UUID id, @Valid @RequestBody TenantRequest request) {
        return ApiResponse.ok(tenantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        tenantService.delete(id);
        return ApiResponse.message("Tenant excluído.");
    }

    @PostMapping("/{id}/admins")
    public ApiResponse<TenantAdminResponse> createAdmin(@PathVariable UUID id, @Valid @RequestBody CreateTenantAdminRequest request) {
        return ApiResponse.ok(tenantService.createAdmin(id, request));
    }
}
