package com.arqly.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class SettingsDtos {
    private SettingsDtos() {}

    public record SmtpSettings(@NotBlank String host, @Min(1) int port, String username, String password,
                               boolean ssl, boolean tls, @Email @NotBlank String senderEmail,
                               @NotBlank String senderName) {}

    public record GeneralSettings(@NotBlank String platformName, @NotBlank String publicUrl,
                                  @NotBlank String frontendUrl, @NotBlank String backendUrl,
                                  @NotBlank String language, @NotBlank String timezone) {}

    public record TestEmailRequest(@Email @NotBlank String to) {}
}
