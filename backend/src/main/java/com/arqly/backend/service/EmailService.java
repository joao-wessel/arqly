package com.arqly.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final SettingsService settingsService;

    public EmailService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void sendFirstAccess(String to, String link) {
        log.info("Primeiro acesso gerado para {}: {}", to, link);
    }

    public void sendPasswordReset(String to, String link) {
        log.info("Recuperação de senha gerada para {}: {}", to, link);
    }

    public void sendTest(String to) {
        settingsService.testSmtp(to);
    }
}
