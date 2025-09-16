package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

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
