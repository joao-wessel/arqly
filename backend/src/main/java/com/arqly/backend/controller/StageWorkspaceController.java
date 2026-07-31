package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistItemRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistItemResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.ChecklistToggleRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.CommentRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.CommentResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.ReorderChecklistRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.StageUpdateRequest;
import com.arqly.backend.dto.StageWorkspaceDtos.StageWorkspaceResponse;
import com.arqly.backend.dto.StageWorkspaceDtos.StageWorkspaceStatsResponse;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.StageWorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/stage-workspaces")
public class StageWorkspaceController {
    private final StageWorkspaceService service;

    public StageWorkspaceController(StageWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public ApiResponse<StageWorkspaceStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.stats(user.getTenantId()));
    }

    @GetMapping("/{stageId}")
    public ApiResponse<StageWorkspaceResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable UUID stageId) {
        return ApiResponse.ok(service.get(user.getTenantId(), stageId));
    }

    @PutMapping("/{stageId}")
    public ApiResponse<StageWorkspaceResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID stageId,
                                                      @Valid @RequestBody StageUpdateRequest request) {
        return ApiResponse.ok(service.updateStage(user.getTenantId(), stageId, user.getId(), user.getUsername(), request));
    }

    @PatchMapping("/{stageId}/complete")
    public ApiResponse<StageWorkspaceResponse> complete(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID stageId) {
        return ApiResponse.ok(service.complete(user.getTenantId(), stageId, user.getId(), user.getUsername()));
    }

    @PatchMapping("/{stageId}/pause")
    public ApiResponse<StageWorkspaceResponse> pause(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable UUID stageId) {
        return ApiResponse.ok(service.pause(user.getTenantId(), stageId, user.getId(), user.getUsername()));
    }

    @PatchMapping("/{stageId}/cancel")
    public ApiResponse<StageWorkspaceResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID stageId) {
        return ApiResponse.ok(service.cancel(user.getTenantId(), stageId, user.getId(), user.getUsername()));
    }

    @PostMapping("/{stageId}/checklist")
    public ApiResponse<ChecklistItemResponse> addChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable UUID stageId,
                                                           @Valid @RequestBody ChecklistItemRequest request) {
        return ApiResponse.ok(service.addChecklistItem(user.getTenantId(), stageId, user.getId(), user.getUsername(), request));
    }

    @PutMapping("/{stageId}/checklist/{itemId}")
    public ApiResponse<ChecklistItemResponse> updateChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable UUID stageId,
                                                              @PathVariable UUID itemId,
                                                              @Valid @RequestBody ChecklistItemRequest request) {
        return ApiResponse.ok(service.updateChecklistItem(user.getTenantId(), stageId, itemId, user.getId(), user.getUsername(), request));
    }

    @PatchMapping("/{stageId}/checklist/{itemId}/toggle")
    public ApiResponse<ChecklistItemResponse> toggleChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable UUID stageId,
                                                              @PathVariable UUID itemId,
                                                              @Valid @RequestBody ChecklistToggleRequest request) {
        return ApiResponse.ok(service.toggleChecklistItem(user.getTenantId(), stageId, itemId, user.getId(), user.getUsername(), request));
    }

    @PostMapping("/{stageId}/checklist/{itemId}/duplicate")
    public ApiResponse<ChecklistItemResponse> duplicateChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @PathVariable UUID stageId,
                                                                 @PathVariable UUID itemId) {
        return ApiResponse.ok(service.duplicateChecklistItem(user.getTenantId(), stageId, itemId, user.getId(), user.getUsername()));
    }

    @PatchMapping("/{stageId}/checklist/{itemId}/delete")
    public ApiResponse<Void> deleteChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable UUID stageId,
                                             @PathVariable UUID itemId) {
        service.deleteChecklistItem(user.getTenantId(), stageId, itemId, user.getId(), user.getUsername());
        return ApiResponse.message("Item removido.");
    }

    @PatchMapping("/{stageId}/checklist/reorder")
    public ApiResponse<List<ChecklistItemResponse>> reorderChecklist(@AuthenticationPrincipal AuthenticatedUser user,
                                                                     @PathVariable UUID stageId,
                                                                     @Valid @RequestBody ReorderChecklistRequest request) {
        return ApiResponse.ok(service.reorderChecklist(user.getTenantId(), stageId, user.getId(), user.getUsername(), request));
    }

    @PostMapping("/{stageId}/comments")
    public ApiResponse<CommentResponse> addComment(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable UUID stageId,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(service.addComment(user.getTenantId(), stageId, user.getId(), user.getUsername(), request));
    }

    @PutMapping("/{stageId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID stageId,
                                                      @PathVariable UUID commentId,
                                                      @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(service.updateComment(user.getTenantId(), stageId, commentId, user.getId(), user.getUsername(), request));
    }

    @PatchMapping("/{stageId}/comments/{commentId}/delete")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID stageId,
                                           @PathVariable UUID commentId) {
        service.deleteComment(user.getTenantId(), stageId, commentId, user.getId(), user.getUsername());
        return ApiResponse.message("Comentário removido.");
    }
}
