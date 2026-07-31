package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingRequest;
import com.arqly.backend.dto.BriefingDtos.BriefingResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingStatsResponse;
import com.arqly.backend.dto.BriefingDtos.BriefingSummaryResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.entity.BriefingStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.BriefingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/briefings")
public class BriefingController {
    private final BriefingService service;

    public BriefingController(BriefingService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<BriefingSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) UUID clientId,
                                                           @RequestParam(required = false) UUID responsibleUserId,
                                                           @RequestParam(required = false) BriefingStatus status,
                                                           Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), search, clientId, responsibleUserId, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BriefingResponse> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(user.getTenantId(), id));
    }

    @PostMapping
    public ApiResponse<BriefingResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody BriefingRequest request) {
        return ApiResponse.ok(service.create(user.getTenantId(), user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BriefingResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody BriefingRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), id, user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.getTenantId(), id);
        return ApiResponse.message("Briefing removido.");
    }

    @PostMapping("/{id}/generate-proposal")
    public ApiResponse<ProposalResponse> generateProposal(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.generateProposal(user.getTenantId(), id, user.getUsername()));
    }

    @GetMapping("/stats")
    public ApiResponse<BriefingStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.stats(user.getTenantId()));
    }
}
