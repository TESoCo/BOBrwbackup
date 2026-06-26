package com.example.controller.web.api;

import com.example.controller.web.ControladorAvance;
import com.example.controller.web.ControladorContratistas;
import com.example.controller.web.ControladorProveedores;
import com.example.domain.Usuario;
import com.example.dto.EmailRequest;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.EmailService;
import com.example.servicioWeb.OAuth2EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/email")
public class OAuth2EmailController {

    @Autowired
    private ControladorProveedores controladorProveedores;

    @Autowired
    private ControladorContratistas controladorContratistas;

    @Autowired
    private ControladorAvance controladorAvance;

    @Autowired
    private OAuth2EmailService oauth2EmailService;  // Para usuarios con Google

    @Autowired
    private EmailService emailService;              // Para usuarios locales (OAuth2 del sistema)

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PostMapping("/send-report")
    public ResponseEntity<?> sendReportEmail(
            @RequestBody EmailRequest emailRequest,
            Authentication authentication) {

        try {
            System.out.println("=== INICIANDO ENVÍO DE CORREO ===");
            System.out.println("Tipo de reporte: " + emailRequest.getReportType());
            System.out.println("Destinatarios: " + emailRequest.getRecipients());

            // Obtener usuario autenticado
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

            if (usuario == null) {
                return ResponseEntity.badRequest().body("Usuario no encontrado");
            }

            System.out.println("Usuario: " + username);
            System.out.println("Auth Provider: " + usuario.getAuthProvider());
            System.out.println("Tiene refresh token: " + (usuario.getGoogleRefreshToken() != null));

            // Obtener destinatarios
            List<String> recipients = new ArrayList<>();

            if (emailRequest.getRecipients() != null && !emailRequest.getRecipients().isEmpty()) {
                recipients.addAll(emailRequest.getRecipients());
            }

            if (emailRequest.getCustomEmail() != null && !emailRequest.getCustomEmail().isEmpty()) {
                recipients.add(emailRequest.getCustomEmail());
            }

            if (recipients.isEmpty()) {
                return ResponseEntity.badRequest().body("No se especificaron destinatarios");
            }

            // Generar el reporte según el tipo
            byte[] excelReport;
            String fileName;

            if ("proveedores".equalsIgnoreCase(emailRequest.getReportType())) {
                excelReport = controladorProveedores.generarReporteProveedoresExcel();
                fileName = "reporte_proveedores.xlsx";
            } else if ("contratistas".equalsIgnoreCase(emailRequest.getReportType())) {
                excelReport = controladorContratistas.generarReporteContratistasExcel();
                fileName = "reporte_contratistas.xlsx";
            } else if ("avances".equalsIgnoreCase(emailRequest.getReportType())) {
                excelReport = controladorAvance.generarReporteAvancesConFiltros(
                        emailRequest.getIdObraSelect(),
                        emailRequest.getIdObraTexto(),
                        emailRequest.getIdUsuario(),
                        emailRequest.getIdAPU(),
                        emailRequest.getFecha()
                );
                fileName = "reporte_avances_filtrados.xlsx";
            } else {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }

            // ✅ DECISIÓN: ¿Qué servicio usar?
            EmailService.EmailResult result;

            if (usuario.getGoogleRefreshToken() != null && !usuario.getGoogleRefreshToken().isEmpty()) {
                // ✅ Usuario con Google → Enviar desde su cuenta
                System.out.println("📧 Enviando desde cuenta Google de: " + username);

                OAuth2EmailService.EmailResult oauth2Result = oauth2EmailService.sendMassEmailWithAttachment(
                        recipients,
                        emailRequest.getSubject(),
                        emailRequest.getMessage(),
                        excelReport,
                        fileName,
                        username
                );

                // Convertir a EmailResult (misma estructura)
                result = convertToEmailResult(oauth2Result);

            } else {
                // ✅ Usuario LOCAL → Usar OAuth2 del sistema (cuenta única)
                System.out.println("📧 Enviando desde cuenta del sistema.");

                result = emailService.sendMassEmailWithAttachment(
                        recipients,
                        emailRequest.getSubject(),
                        emailRequest.getMessage(),
                        excelReport,
                        fileName,
                        username  // Pasamos username para logging
                );
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error al enviar el correo: " + e.getMessage());
        }
    }

    /**
     * Convierte OAuth2EmailService.EmailResult a EmailService.EmailResult
     */
    private EmailService.EmailResult convertToEmailResult(OAuth2EmailService.EmailResult oauth2Result) {
        EmailService.EmailResult result = new EmailService.EmailResult();
        // Nota: EmailResult tiene los mismos campos, pero son clases diferentes
        // Tendrás que crear un método que extraiga los valores
        for (int i = 0; i < oauth2Result.getSuccessCount(); i++) {
            result.incrementSuccess();
        }
        for (int i = 0; i < oauth2Result.getFailedCount(); i++) {
            result.incrementFailed();
        }
        for (String email : oauth2Result.getFailedEmails()) {
            result.addFailedEmail(email);
        }
        return result;
    }
}