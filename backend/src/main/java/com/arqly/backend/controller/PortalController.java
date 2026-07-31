package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ApprovalDtos.ApprovalDecisionRequest;
import com.arqly.backend.dto.ClientDtos.PortalPublicResponse;
import com.arqly.backend.dto.PortalDtos.PortalApprovalResponse;
import com.arqly.backend.dto.PortalDtos.PortalDashboardResponse;
import com.arqly.backend.dto.PortalDtos.PortalDocumentResponse;
import com.arqly.backend.dto.PortalDtos.PortalFileResponse;
import com.arqly.backend.dto.PortalDtos.PortalProjectResponse;
import com.arqly.backend.dto.PortalDtos.PortalProjectSummaryResponse;
import com.arqly.backend.dto.PortalDtos.PortalProposalSummaryResponse;
import com.arqly.backend.dto.ProposalDtos.PortalProposalResponse;
import com.arqly.backend.dto.ProposalDtos.ProposalDecisionRequest;
import com.arqly.backend.dto.ProposalDtos.ProposalResponse;
import com.arqly.backend.service.ClientService;
import com.arqly.backend.service.GeneratedDocumentPdfService;
import com.arqly.backend.service.PortalWorkspaceService;
import com.arqly.backend.service.ProposalService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final PortalWorkspaceService portalService;
    private final GeneratedDocumentPdfService pdfService;

    public PortalController(ClientService service, ProposalService proposalService, PortalWorkspaceService portalService,
                            GeneratedDocumentPdfService pdfService) {
        this.service = service;
        this.proposalService = proposalService;
        this.portalService = portalService;
        this.pdfService = pdfService;
    }

    @GetMapping("/{token}")
    public ApiResponse<PortalPublicResponse> portal(@PathVariable UUID token) {
        return ApiResponse.ok(service.portal(token));
    }

    @GetMapping("/{token}/dashboard")
    public ApiResponse<PortalDashboardResponse> dashboard(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.dashboard(token));
    }

    @GetMapping("/{token}/projects")
    public ApiResponse<List<PortalProjectSummaryResponse>> projects(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.projects(token));
    }

    @GetMapping("/{token}/projects/{projectId}")
    public ApiResponse<PortalProjectResponse> project(@PathVariable UUID token, @PathVariable UUID projectId) {
        return ApiResponse.ok(portalService.project(token, projectId));
    }

    @GetMapping("/{token}/proposal-list")
    public ApiResponse<List<PortalProposalSummaryResponse>> proposals(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.proposals(token));
    }

    @GetMapping("/{token}/documents")
    public ApiResponse<List<PortalDocumentResponse>> documents(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.documents(token));
    }

    @GetMapping("/{token}/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> documentPdf(@PathVariable UUID token, @PathVariable UUID documentId) {
        var document = portalService.documentEntity(token, documentId);
        var filename = document.getTitle().replaceAll("[^\\p{L}\\p{N}._-]+", "-") + "-v" + document.getVersion() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(pdfService.generate(document.getTenant().getId(), documentId));
    }

    @GetMapping("/{token}/files")
    public ApiResponse<List<PortalFileResponse>> files(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.files(token));
    }

    @GetMapping("/{token}/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> fileDownload(@PathVariable UUID token, @PathVariable UUID fileId) {
        var file = portalService.portalFile(token, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getName(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(portalService.fileStream(token, fileId)));
    }

    @GetMapping("/{token}/files/{fileId}/preview")
    public ResponseEntity<InputStreamResource> filePreview(@PathVariable UUID token, @PathVariable UUID fileId) {
        var file = portalService.portalFile(token, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getName(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(portalService.fileStream(token, fileId)));
    }

    @GetMapping("/{token}/approvals")
    public ApiResponse<List<PortalApprovalResponse>> approvals(@PathVariable UUID token) {
        return ApiResponse.ok(portalService.approvals(token));
    }

    @PostMapping("/{token}/approvals/{approvalId}/approve")
    public ApiResponse<PortalApprovalResponse> approve(@PathVariable UUID token, @PathVariable UUID approvalId,
                                                       @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ApiResponse.ok(portalService.approve(token, approvalId, request));
    }

    @PostMapping("/{token}/approvals/{approvalId}/reject")
    public ApiResponse<PortalApprovalResponse> rejectApproval(@PathVariable UUID token, @PathVariable UUID approvalId,
                                                             @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ApiResponse.ok(portalService.reject(token, approvalId, request));
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
