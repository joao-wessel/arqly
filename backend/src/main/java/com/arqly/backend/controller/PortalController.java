package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ClientDtos.PortalPublicResponse;
import com.arqly.backend.dto.ProposalDtos.PortalProposalResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalDecisionRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.service.ClientService;
import com.arqly.backend.service.ProposalService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/portal", "/portal"})
public class PortalController {
    private final ClientService service;
    private final ProposalService proposalService;

    public PortalController(ClientService service, ProposalService proposalService) {
        this.service = service;
        this.proposalService = proposalService;
    }

    @GetMapping("/{token}")
    public ApiResponse<PortalPublicResponse> portal(@PathVariable UUID token) {
        return ApiResponse.ok(service.portal(token));
    }

    @GetMapping("/{token}/proposals/{proposalId}")
    public ApiResponse<PortalProposalResponse> proposal(@PathVariable UUID token, @PathVariable UUID proposalId) {
        return ApiResponse.ok(proposalService.portalProposal(token, proposalId));
    }

    @PostMapping("/{token}/proposals/{proposalId}/accept")
    public ApiResponse<ProposalResponse> accept(@PathVariable UUID token, @PathVariable UUID proposalId,
                                                @RequestBody(required = false) ProposalDecisionRequest request,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.ok(proposalService.accept(token, proposalId, request, clientIp(servletRequest), servletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/{token}/proposals/{proposalId}/reject")
    public ApiResponse<ProposalResponse> reject(@PathVariable UUID token, @PathVariable UUID proposalId,
                                                @RequestBody(required = false) ProposalDecisionRequest request,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.ok(proposalService.reject(token, proposalId, request, clientIp(servletRequest), servletRequest.getHeader("User-Agent")));
    }

    private String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
