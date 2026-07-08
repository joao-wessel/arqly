package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.SettingsDtos.GeneralSettings;
import com.arqly.backend.dto.SettingsDtos.SmtpSettings;
import com.arqly.backend.dto.SettingsDtos.TestEmailRequest;
import com.arqly.backend.service.EmailService;
import com.arqly.backend.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final EmailService emailService;

    public SettingsController(SettingsService settingsService, EmailService emailService) {
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    @GetMapping("/smtp")
    public ApiResponse<SmtpSettings> getSmtp() {
        return ApiResponse.ok(settingsService.getSmtp());
    }

    @PutMapping("/smtp")
    public ApiResponse<SmtpSettings> saveSmtp(@Valid @RequestBody SmtpSettings request) {
        return ApiResponse.ok(settingsService.saveSmtp(request));
    }

    @PostMapping("/smtp/test")
    public ApiResponse<Void> testSmtp(@Valid @RequestBody TestEmailRequest request) {
        emailService.sendTest(request.to());
        return ApiResponse.message("E-mail de teste enviado.");
    }

    @GetMapping("/general")
    public ApiResponse<GeneralSettings> getGeneral() {
        return ApiResponse.ok(settingsService.getGeneral());
    }

    @PutMapping("/general")
    public ApiResponse<GeneralSettings> saveGeneral(@Valid @RequestBody GeneralSettings request) {
        return ApiResponse.ok(settingsService.saveGeneral(request));
    }
}
