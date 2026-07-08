package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.UserPreferenceDtos.AppearancePreferencesRequest;
import com.arqly.backend.dto.UserPreferenceDtos.AppearancePreferencesResponse;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/me")
public class PlatformMeController {
    private final UserPreferenceService service;

    public PlatformMeController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/preferences")
    public ApiResponse<AppearancePreferencesResponse> preferences(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.getPlatformPreferences(user.getId()));
    }

    @PutMapping("/preferences")
    public ApiResponse<AppearancePreferencesResponse> updatePreferences(@AuthenticationPrincipal AuthenticatedUser user,
                                                                        @Valid @RequestBody AppearancePreferencesRequest request) {
        return ApiResponse.ok(service.updatePlatformPreferences(user.getId(), request));
    }
}
