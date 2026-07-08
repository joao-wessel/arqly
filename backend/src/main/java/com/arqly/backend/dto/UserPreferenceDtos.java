package com.arqly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class UserPreferenceDtos {
    private UserPreferenceDtos() {}

    public record AppearancePreferencesRequest(
            @NotBlank @Pattern(regexp = "light|dark") String themeMode,
            @NotBlank @Pattern(regexp = "arqly|terracotta|indigo") String colorPalette
    ) {}

    public record AppearancePreferencesResponse(String themeMode, String colorPalette) {}
}
