package com.example.servicioWeb;

import com.example.domain.Usuario;
import com.example.servicio.UsuarioServicio;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service
public class OAuth2EmailService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2EmailService.class);

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private EncryptionService encryptionService;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.oauth.application.name}")
    private String applicationName;

    @Value("${spring.mail.username}")
    private String systemEmail;

    /**
     * Envía correo desde la cuenta del usuario (OAuth2 Gmail API)
     */
    public EmailResult sendMassEmailWithAttachment(
            List<String> recipients,
            String subject,
            String content,
            byte[] attachment,
            String attachmentName,
            String username) {

        EmailResult result = new EmailResult();

        try {
            // Obtener usuario y su refresh token
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
            if (usuario == null || usuario.getGoogleRefreshToken() == null) {
                logger.error("Usuario {} no tiene refresh token OAuth2", username);
                for (String recipient : recipients) {
                    result.incrementFailed();
                    result.addFailedEmail(recipient);
                }
                return result;
            }

            // Crear Gmail service para este usuario
            Gmail gmailService = crearGmailService(usuario);

            // Enviar a cada destinatario
            for (String recipient : recipients) {
                try {
                    MimeMessage mimeMessage = crearMimeMessage(
                            usuario.getNombreUsuario(),
                            recipient,
                            subject,
                            content,
                            attachment,
                            attachmentName
                    );
                    enviarMensaje(mimeMessage, gmailService);
                    result.incrementSuccess();
                    logger.info("Correo enviado a {} desde cuenta de {}", recipient, username);
                } catch (Exception e) {
                    logger.error("Error enviando a {}: {}", recipient, e.getMessage());
                    result.incrementFailed();
                    result.addFailedEmail(recipient);
                }
            }

        } catch (Exception e) {
            logger.error("Error general: {}", e.getMessage());
            for (String recipient : recipients) {
                result.incrementFailed();
                result.addFailedEmail(recipient);
            }
        }

        return result;
    }

    /**
     * Crea un Gmail service usando el refresh token del usuario
     */
    private Gmail crearGmailService(Usuario usuario) throws Exception {
        String refreshToken = encryptionService.decrypt(usuario.getGoogleRefreshToken());

        Credential credential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        credential.refreshToken();

        return new Gmail.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();
    }

    /**
     * Crea un MimeMessage
     */
    private MimeMessage crearMimeMessage(
            String fromEmail,
            String toEmail,
            String subject,
            String text,
            byte[] attachment,
            String attachmentName) throws jakarta.mail.MessagingException {

        Properties props = new Properties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new jakarta.mail.internet.InternetAddress(fromEmail));
        message.addRecipient(jakarta.mail.Message.RecipientType.TO, new jakarta.mail.internet.InternetAddress(toEmail));
        message.setSubject(subject);

        jakarta.mail.internet.MimeMultipart multipart = new jakarta.mail.internet.MimeMultipart();

        // Cuerpo del mensaje
        jakarta.mail.internet.MimeBodyPart bodyPart = new jakarta.mail.internet.MimeBodyPart();
        bodyPart.setText(text);
        multipart.addBodyPart(bodyPart);

        // Adjunto
        if (attachment != null && attachmentName != null) {
            jakarta.mail.internet.MimeBodyPart attachmentPart = new jakarta.mail.internet.MimeBodyPart();
            attachmentPart.setFileName(attachmentName);
            attachmentPart.setContent(attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);
        return message;
    }

    /**
     * Envía mensaje usando Gmail API
     */
    private void enviarMensaje(MimeMessage mimeMessage, Gmail gmailService) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        gmailService.users().messages().send("me", message).execute();
    }

    // EmailResult inner class
    public static class EmailResult {
        private int successCount = 0;
        private int failedCount = 0;
        private List<String> failedEmails = new java.util.ArrayList<>();

        public void incrementSuccess() { successCount++; }
        public void incrementFailed() { failedCount++; }
        public void addFailedEmail(String email) { failedEmails.add(email); }

        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getFailedEmails() { return failedEmails; }
    }
}