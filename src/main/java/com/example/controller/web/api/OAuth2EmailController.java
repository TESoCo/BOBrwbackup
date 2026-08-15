package com.example.controller.web.api;

import com.example.controller.web.ControladorAvance;
import com.example.controller.web.ControladorContratistas;
import com.example.controller.web.ControladorObras;
import com.example.controller.web.ControladorProveedores;
import com.example.domain.Usuario;
import com.example.dto.EmailRequest;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.OAuth2EmailService;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private ControladorObras controladorObras;

    @Autowired
    @Lazy
    private OAuth2EmailService oauth2EmailService;  // Para usuarios con Google

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PostMapping("/send-report")
    public ResponseEntity<?> sendReportEmail(
            @RequestBody EmailRequest emailRequest,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== INICIANDO ENVÍO DE CORREO ===");
            System.out.println("Tipo de reporte: " + emailRequest.getReportType());
            System.out.println("Destinatarios: " + emailRequest.getRecipients());

            // Obtener usuario autenticado
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

            if (usuario == null) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
                response.put("success", false);
                response.put("message", "No se especificaron destinatarios");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
            } else if ("obra".equalsIgnoreCase(emailRequest.getReportType())) {
                // Usar idObraSelect para obtener el ID de la obra
                Long idObra = emailRequest.getIdObraSelect();
                if (idObra == null) {
                    response.put("success", false);
                    response.put("message", "ID de obra no especificado para el reporte de obra");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                excelReport = controladorObras.generarReporteObraExcelmail(idObra);
                fileName = "reporte_obra.xlsx";
            } else {
                response.put("success", false);
                response.put("message", "Tipo de reporte no válido: " + emailRequest.getReportType());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Verificar que el reporte se generó correctamente
            if (excelReport == null || excelReport.length == 0) {
                response.put("success", false);
                response.put("message", "No se pudo generar el reporte");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

                response.put("success", true);
                response.put("successCount", result.getSuccessCount());
                response.put("failedCount", result.getFailedCount());
                response.put("failedEmails", result.getFailedEmails());

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
                response.put("success", false);
                response.put("message", "No se pudo enviar el correo a ningún destinatario");
                response.put("failedCount", result.getFailedCount());
                response.put("failedEmails", result.getFailedEmails());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }


        } catch (Exception e) {
            System.err.println("❌ Error en sendReportEmail: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al enviar el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}