package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.FileDtos.FileMoveRequest;
import com.arqly.backend.dto.FileDtos.FileResponse;
import com.arqly.backend.dto.FileDtos.FileUpdateRequest;
import com.arqly.backend.dto.FileDtos.FileUploadMetadata;
import com.arqly.backend.dto.FileDtos.FileVersionResponse;
import com.arqly.backend.dto.FileDtos.FolderRequest;
import com.arqly.backend.dto.FileDtos.FolderResponse;
import com.arqly.backend.entity.FileOwnerType;
import com.arqly.backend.entity.FileResourceStatus;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.FileManagementService;
import com.arqly.backend.service.FileSearchService;
import com.arqly.backend.service.FolderService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tenant/files")
public class FileController {
    private final FileManagementService service;
    private final FileSearchService searchService;
    private final FolderService folderService;

    public FileController(FileManagementService service, FileSearchService searchService, FolderService folderService) {
        this.service = service;
        this.searchService = searchService;
        this.folderService = folderService;
    }

    @GetMapping
    public ApiResponse<Page<FileResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                @RequestParam(required = false) FileOwnerType ownerType,
                                                @RequestParam(required = false) UUID ownerId,
                                                @RequestParam(required = false) UUID folderId,
                                                @RequestParam(required = false) Boolean rootOnly,
                                                @RequestParam(required = false) String search,
                                                @RequestParam(required = false) String extension,
                                                @RequestParam(required = false) String author,
                                                @RequestParam(required = false) String tag,
                                                @RequestParam(required = false) FileResourceStatus status,
                                                Pageable pageable) {
        return ApiResponse.ok(searchService.search(user.getTenantId(), ownerType, ownerId, folderId, rootOnly,
                search, extension, author, tag, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResponse> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(user.getTenantId(), id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<FileResponse>> upload(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @RequestPart("metadata") @Valid FileUploadMetadata metadata,
                                                   @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.ok(files.stream()
                .map(file -> service.upload(user.getTenantId(), user.getId(), user.getUsername(), file, metadata))
                .toList());
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> newVersion(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                 @RequestPart("file") MultipartFile file,
                                                 @RequestParam(required = false) String comment) {
        return ApiResponse.ok(service.newVersion(user.getTenantId(), user.getId(), user.getUsername(), id, file, comment));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<FileVersionResponse>> versions(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @PathVariable UUID id) {
        return ApiResponse.ok(service.versions(user.getTenantId(), id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @PathVariable UUID id,
                                                         @RequestParam(required = false) UUID versionId) {
        var file = service.get(user.getTenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(service.download(user.getTenantId(), user.getId(), user.getUsername(), id, versionId)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<InputStreamResource> preview(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        var file = service.get(user.getTenantId(), id);
        if (!file.previewAvailable()) return ResponseEntity.status(415).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(service.download(user.getTenantId(), user.getId(), user.getUsername(), id, null)));
    }

    @PutMapping("/{id}")
    public ApiResponse<FileResponse> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                             @Valid @RequestBody FileUpdateRequest request) {
        return ApiResponse.ok(service.update(user.getTenantId(), user.getId(), user.getUsername(), id, request));
    }

    @PutMapping("/{id}/move")
    public ApiResponse<FileResponse> move(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                           @Valid @RequestBody FileMoveRequest request) {
        return ApiResponse.ok(service.move(user.getTenantId(), user.getId(), user.getUsername(), id, request));
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<FileResponse> archive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.changeStatus(user.getTenantId(), user.getId(), user.getUsername(), id,
                FileResourceStatus.ARCHIVED));
    }

    @PatchMapping("/{id}/restore")
    public ApiResponse<FileResponse> restore(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.changeStatus(user.getTenantId(), user.getId(), user.getUsername(), id,
                FileResourceStatus.ACTIVE));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<FileResponse> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.changeStatus(user.getTenantId(), user.getId(), user.getUsername(), id,
                FileResourceStatus.DELETED));
    }

    @GetMapping("/folders")
    public ApiResponse<List<FolderResponse>> folders(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @RequestParam FileOwnerType ownerType,
                                                      @RequestParam UUID ownerId) {
        return ApiResponse.ok(folderService.list(user.getTenantId(), ownerType, ownerId));
    }

    @PostMapping("/folders")
    public ApiResponse<FolderResponse> createFolder(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody FolderRequest request) {
        return ApiResponse.ok(folderService.create(user.getTenantId(), request));
    }

    @PutMapping("/folders/{id}")
    public ApiResponse<FolderResponse> renameFolder(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable UUID id, @RequestBody java.util.Map<String, String> request) {
        return ApiResponse.ok(folderService.rename(user.getTenantId(), id, request.get("name")));
    }

    @DeleteMapping("/folders/{id}")
    public ApiResponse<Void> deleteFolder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        folderService.delete(user.getTenantId(), id);
        return ApiResponse.message("Pasta removida.");
    }

    @GetMapping("/tags")
    public ApiResponse<List<com.arqly.backend.dto.FileDtos.FileTagResponse>> tags(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.tags(user.getTenantId()));
    }
}
