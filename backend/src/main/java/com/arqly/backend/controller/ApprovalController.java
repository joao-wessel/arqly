package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ApprovalDtos.ApprovalRequest;
import com.arqly.backend.dto.ApprovalDtos.ApprovalResponse;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/approvals")
public class ApprovalController {
    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<List<ApprovalResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID projectId) {
        return ApiResponse.ok(service.list(user.getTenantId(), projectId));
    }

    @PostMapping
    public ApiResponse<ApprovalResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody ApprovalRequest request) {
        return ApiResponse.ok(service.create(user.getTenantId(), user.getId(), user.getUsername(), request));
    }
}
