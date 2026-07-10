package com.arqly.backend.service;

import com.arqly.backend.dto.SettingsDtos.SmtpSettings;
import jakarta.mail.MessagingException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final SettingsService settingsService;

    public EmailService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void sendFirstAccess(String to, String link) {
        sendActionEmail(
                to,
                "Primeiro acesso - Arqly",
                "Bem-vindo ao Arqly",
                "Você recebeu um convite para acessar seu escritório no Arqly. Defina sua senha para começar.",
                "Definir minha senha",
                link,
                "Este link expira em 1 hora. Se você não esperava este convite, ignore este e-mail."
        );
        log.info("Primeiro acesso enviado para {}. Link expira em 1 hora.", to);
    }

    public void sendPasswordReset(String to, String link) {
        sendActionEmail(
                to,
                "Recuperação de senha - Arqly",
                "Redefinição de senha",
                "Recebemos uma solicitação para redefinir sua senha. Use o botão abaixo para criar uma nova.",
                "Redefinir senha",
                link,
                "Se você não solicitou a alteração, ignore este e-mail."
        );
        log.info("Recuperação de senha enviada para {}.", to);
    }

    public void sendTest(String to) {
        sendActionEmail(
                to,
                "Teste de envio - Arqly",
                "Servidor SMTP configurado",
                "Este e-mail confirma que o Arqly conseguiu enviar mensagens usando as configurações atuais.",
                "Abrir Arqly",
                settingsService.getGeneral().frontendUrl(),
                "Tudo certo por aqui."
        );
    }

    private void sendActionEmail(String to, String subject, String title, String text, String buttonLabel, String link, String footnote) {
        try {
            SmtpSettings settings = settingsService.getSmtp();
            var sender = sender(settings);
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(settings.senderEmail(), settings.senderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText(title, text, buttonLabel, link, footnote), html(title, text, buttonLabel, link, footnote));
            helper.addInline("arqlyLogo", new ClassPathResource("email/arqly-logo.png"), "image/png");
            sender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Não foi possível montar o e-mail.", ex);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("Remetente de e-mail inválido.", ex);
        }
    }

    private JavaMailSenderImpl sender(SmtpSettings settings) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(settings.username() != null && !settings.username().isBlank()));
        props.put("mail.smtp.ssl.enable", String.valueOf(settings.ssl()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.tls()));
        return sender;
    }

    private String plainText(String title, String text, String buttonLabel, String link, String footnote) {
        return title + "\n\n" + text + "\n\n" + buttonLabel + ": " + link + "\n\n" + footnote;
    }

    private String html(String title, String text, String buttonLabel, String link, String footnote) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;background:#f3f7f6;padding:32px;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;margin:0 auto;">
                    <tr>
                      <td style="padding:0 0 20px;">
                        <img src="cid:arqlyLogo" width="174" height="48" alt="Arqly" style="display:block;border:0;outline:none;text-decoration:none;width:174px;height:auto;">
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#ffffff;border:1px solid #dbe7e4;border-radius:28px;overflow:hidden;box-shadow:0 24px 70px rgba(15,23,42,.10);">
                        <div style="height:8px;background:#00796b;"></div>
                        <div style="padding:38px;">
                          <p style="margin:0 0 12px;color:#00796b;font-size:12px;font-weight:800;letter-spacing:.18em;text-transform:uppercase;">Acesso seguro</p>
                          <h1 style="margin:0;font-size:32px;line-height:1.15;letter-spacing:-.02em;">%s</h1>
                          <p style="margin:16px 0 28px;color:#526174;font-size:16px;line-height:1.65;">%s</p>
                          <a href="%s" style="display:inline-block;background:#00796b;color:#ffffff;text-decoration:none;border-radius:14px;padding:14px 22px;font-size:14px;font-weight:800;">%s</a>
                          <p style="margin:28px 0 0;color:#64748b;font-size:13px;line-height:1.6;">%s</p>
                          <p style="margin:18px 0 0;color:#94a3b8;font-size:12px;line-height:1.6;">Se o botão não funcionar, copie e cole este link no navegador:<br><span style="word-break:break-all;color:#64748b;">%s</span></p>
                        </div>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, text, link, buttonLabel, footnote, link);
    }
}
