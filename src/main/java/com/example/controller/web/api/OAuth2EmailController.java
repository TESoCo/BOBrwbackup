package com.example.controller.web.api;

import com.example.controller.web.ControladorAvance;
import com.example.controller.web.ControladorContratistas;
import com.example.controller.web.ControladorProveedores;
import com.example.domain.Usuario;
import com.example.dto.EmailRequest;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.OAuth2EmailService;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Lazy
    private OAuth2EmailService oauth2EmailService;  // Para usuarios con Google

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

            // VERIFICAR QUE EL USUARIO SEA OAuth2 (Google o Microsoft)
            if (usuario.getAuthProvider() == Usuario.AuthProvider.LOCAL) {
                String mensaje = """
                        No puedes enviar correos desde una cuenta LOCAL.
                        
                        Para enviar reportes por correo, debes:
                        1. Registrar tu cuenta con Google o Microsoft
                        2. O contactar a un administrador para que vincule tu cuenta
                        
                        Actualmente estás autenticado como: %s
                        """.formatted(username);

                return ResponseEntity.status(403).body(mensaje);
            }

            // VERIFICAR QUE TENGA REFRESH TOKEN
            if (usuario.getGoogleRefreshToken() == null || usuario.getGoogleRefreshToken().isEmpty()) {
                return ResponseEntity.status(403).body(
                        "Tu cuenta no tiene un refresh token válido. " +
                                "Por favor, vuelve a iniciar sesión con Google o Microsoft."
                );
            }

            // Obtener destinatarios
            List<String> recipients = new ArrayList<>();

            if (emailRequest.getRecipients() != null && !emailRequest.getRecipients().isEmpty()) {
                recipients.addAll(emailRequest.getRecipients());
            }

            if (emailRequest.getCustomEmail() != null && !emailRequest.getCustomEmail().isEmpty()) {
                String[] customEmails = emailRequest.getCustomEmail().split(",");
                for (String email : customEmails) {
                    String trimmed = email.trim();
                    if (!trimmed.isEmpty() && !recipients.contains(trimmed)) {
                        recipients.add(trimmed);
                    }
                }
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

            // ENVIAR DESDE LA CUENTA DEL USUARIO OAuth2
            OAuth2EmailService.EmailResult result = oauth2EmailService.sendMassEmailWithAttachment(
                    recipients,
                    emailRequest.getSubject(),
                    emailRequest.getMessage(),
                    excelReport,
                    fileName,
                    username
            );



            if (result.getSuccessCount() > 0) {

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("successCount", result.getSuccessCount());

                String emailFrom = usuario.getPersona() != null ?
                        usuario.getPersona().getCorreo() :
                        usuario.getNombreUsuario() + "@gmail.com";

                String successMsg = String.format(
                        "Reporte enviado exitosamente a %d destinatario(s) desde %s",
                        result.getSuccessCount(),
                        emailFrom
                );

                if (result.getFailedCount() > 0) {
                    successMsg += String.format(" ⚠️ %d correo(s) fallaron", result.getFailedCount());
                }

                response.put("message", successMsg);

                return ResponseEntity.ok(response);

            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "❌ No se pudo enviar el correo a ningún destinatario");
                return ResponseEntity.status(500).body(response);
            }


        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error al enviar el correo: " + e.getMessage());
        }
    }


}