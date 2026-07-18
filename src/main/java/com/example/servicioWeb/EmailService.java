package com.example.servicioWeb;

import com.example.domain.Usuario;
import com.example.servicio.UsuarioServicio;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;



import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // ============================================
    // CONFIGURACIÓN DE LA CUENTA DEDICADA DEL SISTEMA
    // ============================================
    @Value("${spring.mail.oauth2.system.client-id}")
    private String clientId;

    @Value("${spring.mail.oauth2.system.client-secret}")
    private String clientSecret;

    @Value("${spring.mail.oauth2.system.refresh-token}")
    private String systemRefreshToken;

    @Value("${spring.mail.oauth2.system.username}")
    private String systemEmail;  // ← CUENTA DEDICADA: sistema@tuempresa.com

    @Value("${spring.mail.oauth2.system.application-name:COB-System}")
    private String applicationName;



 /*   @Value("${spring.mail.oauth2.client-id}")
    private String clientId;

    @Value("${spring.mail.oauth2.client-secret}")
    private String clientSecret;

    @Value("${spring.mail.oauth2.refresh-token}")
    private String systemRefreshToken;

    @Value("${spring.mail.username}")
    private String systemEmail;

    @Value("${spring.mail.oauth2.application-name:BOB}")
    private String applicationName;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private UsuarioServicio usuarioServicio;
*/
    /**
     * Envía un correo usando OAuth2 del sistema
     * (para usuarios locales que no tienen cuenta Google)
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
            // Crear JavaMailSender con OAuth2 del sistema
            JavaMailSender mailSender = crearMailSenderSistema();

            for (String recipient : recipients) {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true);

                    helper.setFrom(systemEmail);
                    helper.setTo(recipient);
                    helper.setSubject(subject);
                    helper.setText(content);

                    if (attachment != null && attachmentName != null) {
                        helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
                    }

                    mailSender.send(message);
                    result.incrementSuccess();
                    logger.info("Correo enviado a {} desde cuenta del sistema (usuario: {})", recipient, username);

                } catch (Exception e) {
                    logger.error("Error enviando a {}: {}", recipient, e.getMessage());
                    result.incrementFailed();
                    result.addFailedEmail(recipient);
                }
            }

        } catch (Exception e) {
            logger.error("Error general en OAuth2 del sistema: {}", e.getMessage());
            for (String recipient : recipients) {
                result.incrementFailed();
                result.addFailedEmail(recipient);
            }
        }

        return result;
    }

    /**
     * Crea JavaMailSender con OAuth2 usando la cuenta del sistema
     */
    private JavaMailSender crearMailSenderSistema() throws Exception {
        // Crear credenciales con refresh token del sistema
        Credential credential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(systemRefreshToken);

        // Refrescar token automáticamente
        credential.refreshToken();
        String accessToken = credential.getAccessToken();

        logger.info("Access Token obtenido para cuenta del sistema: {}", systemEmail);

        // Configurar sesión con autenticación XOAUTH2
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        props.put("mail.smtp.sasl.enable", "true");
        props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
        props.put("mail.smtp.sasl.security", "false");
        props.put("mail.smtp.debug", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // XOAUTH2: email como usuario y access token como contraseña
                return new PasswordAuthentication(systemEmail, accessToken);
            }
        });

        // Configurar mail sender
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setSession(session);
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(systemEmail);
        mailSender.setPassword(accessToken);

        Properties javaMailProps = mailSender.getJavaMailProperties();
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        javaMailProps.put("mail.smtp.sasl.enable", "true");
        javaMailProps.put("mail.smtp.sasl.mechanisms", "XOAUTH2");

        logger.info("✅ JavaMailSender OAuth2 del sistema creado correctamente");
        return mailSender;
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