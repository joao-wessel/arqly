package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.DocumentDtos.DocumentPreviewRequest;
import com.arqly.backend.dto.DocumentDtos.DocumentPreviewResponse;
import com.arqly.backend.dto.DocumentDtos.DocumentTemplateRequest;
import com.arqly.backend.dto.DocumentDtos.DocumentTemplateResponse;
import com.arqly.backend.dto.DocumentDtos.DocumentTemplateSummaryResponse;
import com.arqly.backend.dto.DocumentDtos.DocumentVariableResponse;
import com.arqly.backend.dto.DocumentDtos.GenerateDocumentRequest;
import com.arqly.backend.dto.DocumentDtos.GeneratedDocumentResponse;
import com.arqly.backend.dto.DocumentDtos.GeneratedDocumentSummaryResponse;
import com.arqly.backend.dto.DocumentDtos.NewDocumentVersionRequest;
import com.arqly.backend.entity.DocumentCategory;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.DocumentGenerationService;
import com.arqly.backend.service.GeneratedDocumentPdfService;
import com.arqly.backend.service.DocumentTemplateService;
import com.arqly.backend.service.document.DocumentVariableRegistry;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/tenant/documents")
public class DocumentController {
    private final DocumentTemplateService templateService;
    private final DocumentGenerationService generationService;
    private final DocumentVariableRegistry variableRegistry;
    private final GeneratedDocumentPdfService pdfService;

    public DocumentController(DocumentTemplateService templateService, DocumentGenerationService generationService,
                              DocumentVariableRegistry variableRegistry, GeneratedDocumentPdfService pdfService) {
        this.templateService = templateService;
        this.generationService = generationService;
        this.variableRegistry = variableRegistry;
        this.pdfService = pdfService;
    }

    @GetMapping("/templates")
    public ApiResponse<Page<DocumentTemplateSummaryResponse>> templates(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @RequestParam(required = false) String search,
                                                                         @RequestParam(required = false) DocumentCategory category,
                                                                         @RequestParam(required = false) Boolean active,
                                                                         Pageable pageable) {
        return ApiResponse.ok(templateService.list(user.getTenantId(), search, category, active, pageable));
    }

    @GetMapping("/templates/options")
    public ApiResponse<List<DocumentTemplateSummaryResponse>> templateOptions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(templateService.options(user.getTenantId()));
    }

    @GetMapping("/templates/{id}")
    public ApiResponse<DocumentTemplateResponse> template(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(templateService.get(user.getTenantId(), id));
    }

    @GetMapping("/templates/{id}/versions")
    public ApiResponse<List<DocumentTemplateResponse>> templateVersions(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @PathVariable UUID id) {
        return ApiResponse.ok(templateService.versions(user.getTenantId(), id));
    }

    @PostMapping("/templates")
    public ApiResponse<DocumentTemplateResponse> createTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @Valid @RequestBody DocumentTemplateRequest request) {
        return ApiResponse.ok(templateService.create(user.getTenantId(), request));
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<DocumentTemplateResponse> updateTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @PathVariable UUID id,
                                                                 @Valid @RequestBody DocumentTemplateRequest request) {
        return ApiResponse.ok(templateService.update(user.getTenantId(), id, request));
    }

    @PostMapping("/templates/{id}/versions")
    public ApiResponse<DocumentTemplateResponse> newTemplateVersion(@AuthenticationPrincipal AuthenticatedUser user,
                                                                     @PathVariable UUID id,
                                                                     @Valid @RequestBody DocumentTemplateRequest request) {
        return ApiResponse.ok(templateService.newVersion(user.getTenantId(), id, request));
    }

    @PostMapping("/templates/{id}/duplicate")
    public ApiResponse<DocumentTemplateResponse> duplicateTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable UUID id) {
        return ApiResponse.ok(templateService.duplicate(user.getTenantId(), id));
    }

    @PatchMapping("/templates/{id}/activate")
    public ApiResponse<DocumentTemplateResponse> activateTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @PathVariable UUID id) {
        return ApiResponse.ok(templateService.setActive(user.getTenantId(), id, true));
    }

    @PatchMapping("/templates/{id}/deactivate")
    public ApiResponse<DocumentTemplateResponse> deactivateTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                     @PathVariable UUID id) {
        return ApiResponse.ok(templateService.setActive(user.getTenantId(), id, false));
    }

    @PatchMapping("/templates/{id}/archive")
    public ApiResponse<DocumentTemplateResponse> archiveTemplate(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID id) {
        return ApiResponse.ok(templateService.archive(user.getTenantId(), id));
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        templateService.delete(user.getTenantId(), id);
        return ApiResponse.message("Modelo removido.");
    }

    @PostMapping("/preview")
    public ApiResponse<DocumentPreviewResponse> preview(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody DocumentPreviewRequest request) {
        return ApiResponse.ok(generationService.preview(user.getTenantId(), request));
    }

    @GetMapping("/generated")
    public ApiResponse<Page<GeneratedDocumentSummaryResponse>> generated(@AuthenticationPrincipal AuthenticatedUser user,
                                                                          @RequestParam(required = false) String search,
                                                                          @RequestParam(required = false) DocumentCategory category,
                                                                          @RequestParam(required = false) GeneratedDocumentStatus status,
                                                                          @RequestParam(required = false) UUID projectId,
                                                                          @RequestParam(required = false) UUID proposalId,
                                                                          @RequestParam(required = false) UUID clientId,
                                                                          Pageable pageable) {
        return ApiResponse.ok(generationService.list(user.getTenantId(), search, category, status, projectId, proposalId, clientId, pageable));
    }

    @GetMapping("/generated/{id}")
    public ApiResponse<GeneratedDocumentResponse> generatedDocument(@AuthenticationPrincipal AuthenticatedUser user,
                                                                     @PathVariable UUID id) {
        return ApiResponse.ok(generationService.get(user.getTenantId(), id));
    }

    @GetMapping("/generated/{id}/pdf")
    public ResponseEntity<byte[]> generatedDocumentPdf(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID id) {
        var generated = generationService.get(user.getTenantId(), id);
        var filename = generated.title().replaceAll("[^\\p{L}\\p{N}._-]+", "-") + "-v" + generated.version() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(pdfService.generate(user.getTenantId(), id));
    }

    @GetMapping("/generated/{id}/versions")
    public ApiResponse<List<GeneratedDocumentResponse>> generatedVersions(@AuthenticationPrincipal AuthenticatedUser user,
                                                                           @PathVariable UUID id) {
        return ApiResponse.ok(generationService.versions(user.getTenantId(), id));
    }

    @PostMapping("/generated")
    public ApiResponse<GeneratedDocumentResponse> generate(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody GenerateDocumentRequest request) {
        return ApiResponse.ok(generationService.generate(user.getTenantId(), user.getId(), user.getUsername(), request));
    }

    @PostMapping("/generated/{id}/versions")
    public ApiResponse<GeneratedDocumentResponse> newGeneratedVersion(@AuthenticationPrincipal AuthenticatedUser user,
                                                                       @PathVariable UUID id,
                                                                       @Valid @RequestBody NewDocumentVersionRequest request) {
        return ApiResponse.ok(generationService.newVersion(user.getTenantId(), id, user.getId(), user.getUsername(), request));
    }

    @PostMapping("/generated/{id}/duplicate")
    public ApiResponse<GeneratedDocumentResponse> duplicateGenerated(@AuthenticationPrincipal AuthenticatedUser user,
                                                                      @PathVariable UUID id) {
        return ApiResponse.ok(generationService.duplicate(user.getTenantId(), id, user.getId(), user.getUsername()));
    }

    @PatchMapping("/generated/{id}/archive")
    public ApiResponse<GeneratedDocumentResponse> archiveGenerated(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID id) {
        return ApiResponse.ok(generationService.archive(user.getTenantId(), id, user.getId(), user.getUsername()));
    }

    @PatchMapping("/generated/{id}/publish")
    public ApiResponse<GeneratedDocumentResponse> publishGenerated(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable UUID id) {
        return ApiResponse.ok(generationService.publishToPortal(user.getTenantId(), id));
    }

    @PatchMapping("/generated/{id}/hide")
    public ApiResponse<GeneratedDocumentResponse> hideGenerated(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @PathVariable UUID id) {
        return ApiResponse.ok(generationService.hideFromPortal(user.getTenantId(), id));
    }

    @GetMapping("/variables")
    public ApiResponse<List<DocumentVariableResponse>> variables() {
        return ApiResponse.ok(variableRegistry.definitions().stream()
                .map(item -> new DocumentVariableResponse(item.group(), item.label(), item.placeholder(), item.description()))
                .toList());
    }

    @GetMapping("/categories")
    public ApiResponse<List<DocumentCategory>> categories() {
        return ApiResponse.ok(Arrays.asList(DocumentCategory.values()));
    }
}
