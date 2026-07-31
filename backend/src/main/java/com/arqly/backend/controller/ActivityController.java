package com.arqly.backend.controller;

import com.arqly.backend.dto.ActivityDtos.ActivityResponse;
import com.arqly.backend.dto.ActivityDtos.CommentActivityRequest;
import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ActivityService;
import com.arqly.backend.service.ActivityService.ActivityFilter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/tenant/activities")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<ActivityResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @RequestParam(required = false) UUID projectId,
                                                    @RequestParam(required = false) UUID clientId,
                                                    @RequestParam(required = false) UUID proposalId,
                                                    @RequestParam(required = false) UUID stageId,
                                                    @RequestParam(defaultValue = "ALL") ActivityFilter filter,
                                                    @RequestParam(required = false) String search,
                                                    Pageable pageable) {
        return ApiResponse.ok(service.list(user.getTenantId(), projectId, clientId, proposalId, stageId, filter, search, pageable));
    }

    @PostMapping("/comments")
    public ApiResponse<ActivityResponse> addComment(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @RequestParam UUID projectId,
                                                    @RequestParam(required = false) UUID phaseId,
                                                    @RequestParam(required = false) UUID stageId,
                                                    @Valid @RequestBody CommentActivityRequest request) {
        return ApiResponse.ok(service.addComment(user.getTenantId(), projectId, phaseId, stageId, user.getId(), user.getUsername(), request.comment()));
    }

    @PutMapping("/{activityId}/comments")
    public ApiResponse<ActivityResponse> updateComment(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @PathVariable UUID activityId,
                                                       @Valid @RequestBody CommentActivityRequest request) {
        return ApiResponse.ok(service.updateComment(user.getTenantId(), activityId, user.getId(), request.comment()));
    }

    @PatchMapping("/{activityId}/comments/delete")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID activityId) {
        service.deleteComment(user.getTenantId(), activityId, user.getId());
        return ApiResponse.message("Comentário removido.");
    }
}
