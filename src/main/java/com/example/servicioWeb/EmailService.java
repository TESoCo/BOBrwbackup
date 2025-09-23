package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    // Método existente para correo simple
    public boolean sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Correo enviado exitosamente a: {}", to);
            return true;
        } catch (Exception e) {
            logger.error("Error enviando correo a {}: {}", to, e.getMessage());
            return false;
        }
    }

    // Nuevo método para enviar correo con archivo adjunto
    public boolean sendEmailWithAttachment(String to, String subject, String text,
                                           byte[] attachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            // Adjuntar archivo
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment));

            mailSender.send(message);
            logger.info("Correo con adjunto enviado exitosamente a: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Error enviando correo con adjunto a {}: {}", to, e.getMessage());
            return false;
        }
    }

    // Método para envío masivo con adjuntos
    public EmailResult sendMassEmailWithAttachment(List<String> recipients, String subject,
                                                   String content, byte[] attachment,
                                                   String attachmentName) {
        EmailResult result = new EmailResult();

        for (String recipient : recipients) {
            if (sendEmailWithAttachment(recipient, subject, content, attachment, attachmentName)) {
                result.incrementSuccess();
            } else {
                result.incrementFailed();
                result.addFailedEmail(recipient);
            }
        }

        logger.info("Resultado envío masivo con adjuntos: {} exitosos, {} fallidos",
                result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    // Método existente para envío masivo sin adjuntos
    public EmailResult sendMassEmail(List<String> recipients, String subject, String content) {
        EmailResult result = new EmailResult();

        for (String recipient : recipients) {
            if (sendSimpleEmail(recipient, subject, content)) {
                result.incrementSuccess();
            } else {
                result.incrementFailed();
                result.addFailedEmail(recipient);
            }
        }

        logger.info("Resultado envío masivo: {} exitosos, {} fallidos",
                result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    public static class EmailResult {
        private int successCount;
        private int failedCount;
        private List<String> failedEmails;

        public EmailResult() {
            this.successCount = 0;
            this.failedCount = 0;
            this.failedEmails = new java.util.ArrayList<>();
        }

        public void incrementSuccess() { successCount++; }
        public void incrementFailed() { failedCount++; }
        public void addFailedEmail(String email) { failedEmails.add(email); }

        // Getters
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getFailedEmails() { return failedEmails; }
    }
}