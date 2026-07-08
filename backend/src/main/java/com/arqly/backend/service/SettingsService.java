package com.arqly.backend.service;

import com.arqly.backend.dto.SettingsDtos.GeneralSettings;
import com.arqly.backend.dto.SettingsDtos.SmtpSettings;
import com.arqly.backend.entity.PlatformSetting;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.repository.PlatformSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private static final String SMTP_KEY = "smtp";
    private static final String GENERAL_KEY = "general";
    private final PlatformSettingRepository repository;
    private final ObjectMapper objectMapper;

    public SettingsService(PlatformSettingRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SmtpSettings saveSmtp(SmtpSettings settings) {
        save(SMTP_KEY, settings);
        return settings;
    }

    public SmtpSettings getSmtp() {
        return read(SMTP_KEY, SmtpSettings.class, new SmtpSettings("localhost", 1025, "", "", false, false, "noreply@arqly.local", "Arqly"));
    }

    @Transactional
    public GeneralSettings saveGeneral(GeneralSettings settings) {
        save(GENERAL_KEY, settings);
        return settings;
    }

    public GeneralSettings getGeneral() {
        return read(GENERAL_KEY, GeneralSettings.class,
                new GeneralSettings("Arqly", "http://localhost", "http://localhost:4200", "http://localhost:8080", "pt-BR", "America/Sao_Paulo"));
    }

    public void testSmtp(String to) {
        SmtpSettings settings = getSmtp();
        var sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(settings.username() != null && !settings.username().isBlank()));
        props.put("mail.smtp.ssl.enable", String.valueOf(settings.ssl()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.tls()));

        var message = new SimpleMailMessage();
        message.setFrom(settings.senderEmail());
        message.setTo(to);
        message.setSubject("Teste de envio - Arqly");
        message.setText("Servidor SMTP configurado com sucesso.");
        sender.send(message);
    }

    private void save(String key, Object value) {
        try {
            var setting = repository.findById(key).orElseGet(PlatformSetting::new);
            setting.setKey(key);
            setting.setValue(objectMapper.writeValueAsString(value));
            repository.save(setting);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Não foi possível salvar a configuração.");
        }
    }

    private <T> T read(String key, Class<T> type, T fallback) {
        return repository.findById(key)
                .map(PlatformSetting::getValue)
                .map(value -> {
                    try {
                        return objectMapper.readValue(value, type);
                    } catch (JsonProcessingException ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
