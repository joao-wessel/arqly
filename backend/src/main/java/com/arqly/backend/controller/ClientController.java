package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ClientDtos.ClientPortalAccessResponse;
import com.arqly.backend.dto.ClientDtos.ClientRequest;
import com.arqly.backend.dto.ClientDtos.ClientResponse;
import com.arqly.backend.dto.ClientDtos.ClientSummaryResponse;
import com.arqly.backend.dto.ClientDtos.PortalValidityRequest;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.ClientStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/clients")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<ClientSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(required = false) String document,
                                                         @RequestParam(required = false) ClientStatus status,
                                                         @RequestParam(required = false) String city,
                                                         @RequestParam(required = false) ClientPersonType personType,
                                                         @RequestParam(required = false) Boolean portal,
                                                         Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), name, document, status, city, personType, portal, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientResponse> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(user.getTenantId(), id));
    }

    @PostMapping
    public ApiResponse<ClientResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                              @Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(service.create(user.getTenantId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClientResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.getTenantId(), id);
        return ApiResponse.message("Cliente excluído.");
    }

    @PostMapping("/{id}/portal-access")
    public ApiResponse<ClientPortalAccessResponse> generatePortalAccess(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.generatePortalAccess(user.getTenantId(), id));
    }

    @DeleteMapping("/{id}/portal-access")
    public ApiResponse<ClientPortalAccessResponse> revokePortalAccess(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.revokePortalAccess(user.getTenantId(), id));
    }

    @PatchMapping("/{id}/portal-access/validity")
    public ApiResponse<ClientPortalAccessResponse> updatePortalValidity(@AuthenticationPrincipal AuthenticatedUser user,
                                                                        @PathVariable UUID id,
                                                                        @Valid @RequestBody PortalValidityRequest request) {
        return ApiResponse.ok(service.updatePortalValidity(user.getTenantId(), id, request.expiresAt()));
    }

    @GetMapping("/{id}/portal-access/history")
    public ApiResponse<List<ClientPortalAccessResponse>> portalHistory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.portalHistory(user.getTenantId(), id));
    }
}
