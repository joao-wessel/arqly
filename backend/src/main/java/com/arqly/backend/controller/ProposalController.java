package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ProposalDtos.ProjectCreatedResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalStatsResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalSummaryResponse;
import com.arqly.backend.dto.ProposalDtos.SendProposalRequest;
import com.arqly.backend.entity.ProposalStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ProposalService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/tenant/proposals")
public class ProposalController {
    private final ProposalService service;

    public ProposalController(ProposalService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<ProposalSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @RequestParam(required = false) String number,
                                                           @RequestParam(required = false) UUID clientId,
                                                           @RequestParam(required = false) ProposalStatus status,
                                                           @RequestParam(required = false) LocalDate from,
                                                           @RequestParam(required = false) LocalDate to,
                                                           @RequestParam(required = false) BigDecimal minValue,
                                                           @RequestParam(required = false) BigDecimal maxValue,
                                                           @RequestParam(required = false) String responsible,
                                                           Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), number, clientId, status, from, to, minValue, maxValue, responsible, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProposalResponse> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(user.getTenantId(), id));
    }

    @PostMapping
    public ApiResponse<ProposalResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody ProposalRequest request) {
        return ApiResponse.ok(service.create(user.getTenantId(), user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProposalResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody ProposalRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), id, user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.getTenantId(), id);
        return ApiResponse.message("Proposta removida.");
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<ProposalResponse> duplicate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.duplicate(user.getTenantId(), id, user.getUsername()));
    }

    @PostMapping("/{id}/send")
    public ApiResponse<ProposalResponse> send(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id,
                                              @RequestBody(required = false) SendProposalRequest request) {
        return ApiResponse.ok(service.send(user.getTenantId(), id, request == null ? null : request.message()));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ProposalResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.cancel(user.getTenantId(), id));
    }

    @PatchMapping("/{id}/expire")
    public ApiResponse<ProposalResponse> expire(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.expire(user.getTenantId(), id));
    }

    @PostMapping("/{id}/create-project")
    public ApiResponse<ProjectCreatedResponse> createProject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.createProject(user.getTenantId(), id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        var proposal = service.get(user.getTenantId(), id);
        var body = service.pdf(user.getTenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(proposal.number() + ".pdf").build().toString())
                .body(body);
    }

    @GetMapping("/stats")
    public ApiResponse<ProposalStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.stats(user.getTenantId()));
    }
}
