package com.example.controller.web.api;

import com.example.controller.web.ControladorAvance;
import com.example.controller.web.ControladorContratistas;
import com.example.controller.web.ControladorProveedores;
import com.example.dto.EmailRequest;
import com.example.servicioWeb.OAuth2EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private OAuth2EmailService emailService;

    @PostMapping("/send-report")
    public ResponseEntity<?> sendReportEmail(@RequestBody EmailRequest emailRequest) {
        try {
            System.out.println("=== INICIANDO ENVÍO DE CORREO ===");
            System.out.println("Tipo de reporte: " + emailRequest.getReportType());
            System.out.println("Destinatarios: " + emailRequest.getRecipients());

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

            // Enviar correo usando OAuth2
            OAuth2EmailService.EmailResult result = emailService.sendMassEmailWithAttachment(
                    recipients,
                    emailRequest.getSubject(),
                    emailRequest.getMessage(),
                    excelReport,
                    fileName
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar el correo: " + e.getMessage());
        }
    }
}