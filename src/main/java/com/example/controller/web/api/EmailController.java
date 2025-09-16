package com.example.controller.web.api;
import com.example.servicioWeb.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8888", "https://api.apidog.com"})
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/mass-email")
    public ResponseEntity<?> sendMassEmail(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> recipients = (List<String>) request.get("recipients");
            String subject = (String) request.get("subject");
            String content = (String) request.get("content");

            if (recipients == null || recipients.isEmpty()) {
                return ResponseEntity.badRequest().body("La lista de destinatarios está vacía");
            }

            EmailService.EmailResult result = emailService.sendMassEmail(recipients, subject, content);

            return ResponseEntity.ok(Map.of(
                    "message", "Proceso de envío completado",
                    "successCount", result.getSuccessCount(),
                    "failedCount", result.getFailedCount(),
                    "failedEmails", result.getFailedEmails()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> sendTestEmail() {
        try {
            boolean success = emailService.sendSimpleEmail(
                    "test@example.com",
                    "Email de prueba",
                    "Este es un correo de prueba desde Spring Boot"
            );

            if (success) {
                return ResponseEntity.ok("Correo de prueba enviado exitosamente");
            } else {
                return ResponseEntity.status(500).body("Error al enviar correo de prueba");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
