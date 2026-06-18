package com.example.servicioWeb;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service
public class OAuth2EmailService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2EmailService.class);

    @Autowired
    private Gmail gmailService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${google.oauth.application.name}")
    private String applicationName;

    /**
     * Envía un correo simple sin adjuntos usando OAuth2
     */
    public boolean sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = createMimeMessage(to, subject, text, null, null);
            sendMessage(mimeMessage);
            logger.info("Correo enviado exitosamente a: {}", to);
            return true;
        } catch (Exception e) {
            logger.error("Error enviando correo a {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía un correo con archivo adjunto usando OAuth2
     */
    public boolean sendEmailWithAttachment(String to, String subject, String text,
                                           byte[] attachment, String attachmentName) {
        try {
            MimeMessage mimeMessage = createMimeMessage(to, subject, text, attachment, attachmentName);
            sendMessage(mimeMessage);
            logger.info("Correo con adjunto enviado exitosamente a: {}", to);
            return true;
        } catch (Exception e) {
            logger.error("Error enviando correo con adjunto a {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envío masivo con adjuntos
     */
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

    /**
     * Envío masivo sin adjuntos
     */
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

    /**
     * Crea un MimeMessage para Gmail
     */
    private MimeMessage createMimeMessage(String to, String subject, String text,
                                          byte[] attachment, String attachmentName) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getInstance(props, null);

        MimeMessage mimeMessage = new MimeMessage(session);

        // Configurar destinatarios y remitente
        mimeMessage.setFrom(new InternetAddress(fromEmail));
        mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        mimeMessage.setSubject(subject);

        // Crear el cuerpo del mensaje
        MimeMultipart multipart = new MimeMultipart();

        // Cuerpo del mensaje
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(text);
        multipart.addBodyPart(bodyPart);

        // Adjuntar archivo si existe
        if (attachment != null && attachmentName != null) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setFileName(attachmentName);
            attachmentPart.setContent(attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            multipart.addBodyPart(attachmentPart);
        }

        mimeMessage.setContent(multipart);
        return mimeMessage;
    }

    /**
     * Envía el mensaje usando la API de Gmail
     */
    private void sendMessage(MimeMessage mimeMessage) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);

        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        try {
            // Enviar usando Gmail API
            gmailService.users().messages().send("me", message).execute();
        } catch (GoogleJsonResponseException e) {
            logger.error("Error de Gmail API: {}", e.getDetails());
            throw new IOException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    // Clase EmailResult (manteniendo la misma estructura)
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

        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getFailedEmails() { return failedEmails; }
    }
}