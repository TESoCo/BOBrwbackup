package com.example.controller.web.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailDiagnosticController {

    @Value("${spring.mail.oauth2.system.client-id}")
    private String systemClientId;

    @Value("${spring.mail.oauth2.system.client-secret}")
    private String systemClientSecret;

    @Value("${spring.mail.oauth2.system.refresh-token}")
    private String systemRefreshToken;

    @Value("${spring.mail.oauth2.system.username}")
    private String systemEmail;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    @GetMapping("/diagnostic-full")
    public Map<String, Object> diagnosticFull() {
        Map<String, Object> response = new HashMap<>();

        // 1. Mostrar qué propiedades está usando cada servicio
        Map<String, Object> emailServiceConfig = new HashMap<>();
        emailServiceConfig.put("clientId", systemClientId != null ? systemClientId.substring(0, 15) + "..." : "null");
        emailServiceConfig.put("clientSecret", systemClientSecret != null ? "***" : "null");
        emailServiceConfig.put("refreshToken", systemRefreshToken != null ? systemRefreshToken.substring(0, 20) + "..." : "null");
        emailServiceConfig.put("systemEmail", systemEmail);
        response.put("emailServiceConfig", emailServiceConfig);

        Map<String, Object> oauth2Config = new HashMap<>();
        oauth2Config.put("clientId", googleClientId != null ? googleClientId.substring(0, 15) + "..." : "null");
        oauth2Config.put("clientSecret", googleClientSecret != null ? "***" : "null");
        oauth2Config.put("redirectUri", googleRedirectUri);
        response.put("oauth2Config", oauth2Config);

        // 2. Verificar si los client IDs coinciden
        boolean clientIdsMatch = systemClientId != null && systemClientId.equals(googleClientId);
        response.put("clientIdsMatch", clientIdsMatch);

        if (!clientIdsMatch) {
            response.put("issue", "❌ LOS CLIENT IDs NO COINCIDEN");
            response.put("solution", """
                El refresh token fue generado con un client-id diferente.
                Debes usar el MISMO client-id para el EmailService y el ControladorOAuth2.
                
                Solución: Usa la misma propiedad para ambos:
                ${google.client.id} en lugar de ${spring.mail.oauth2.system.client-id}
                """);
            return response;
        }

        // 3. Intentar usar el refresh token con diferentes configuraciones
        Map<String, String> testResults = new HashMap<>();

        // Test 1: Con el client-id del sistema
        testResults.put("test1_clientId", systemClientId);
        try {
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                    .setInstalled(new GoogleClientSecrets.Details()
                            .setClientId(systemClientId)
                            .setClientSecret(systemClientSecret));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    clientSecrets,
                    Collections.singletonList("https://www.googleapis.com/auth/gmail.send"))
                    .setAccessType("offline")
                    .build();

            var tokenResponse = flow.newTokenRequest(systemRefreshToken)
                    .setGrantType("refresh_token")
                    .execute();

            testResults.put("test1_result", "✅ FUNCIONA");
            testResults.put("test1_accessToken", tokenResponse.getAccessToken() != null ? "✅ Obtenido" : "❌ No obtenido");

        } catch (Exception e) {
            testResults.put("test1_result", "❌ FALLA");
            testResults.put("test1_error", e.getMessage());
        }

        // Test 2: Con el client-id de Google (si es diferente)
        if (!systemClientId.equals(googleClientId)) {
            try {
                GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                        .setInstalled(new GoogleClientSecrets.Details()
                                .setClientId(googleClientId)
                                .setClientSecret(googleClientSecret));

                GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        clientSecrets,
                        Collections.singletonList("https://www.googleapis.com/auth/gmail.send"))
                        .setAccessType("offline")
                        .build();

                var tokenResponse = flow.newTokenRequest(systemRefreshToken)
                        .setGrantType("refresh_token")
                        .execute();

                testResults.put("test2_result", "✅ FUNCIONA CON GOOGLE CLIENT ID");

            } catch (Exception e) {
                testResults.put("test2_result", "❌ TAMBIÉN FALLA");
                testResults.put("test2_error", e.getMessage());
            }
        }

        response.put("testResults", testResults);

        // 4. Análisis y recomendación
        if (testResults.containsKey("test1_result") && testResults.get("test1_result").contains("FUNCIONA")) {
            response.put("conclusion", "✅ El refresh token es válido. El problema debe ser otro.");
        } else {
            response.put("conclusion", """
                ❌ El refresh token NO es válido para ningún client-id.
                
                Esto significa que el token fue:
                1. Revocado manualmente desde https://myaccount.google.com/permissions
                2. Generado con un client-id completamente diferente
                3. O simplemente ya no es válido
                
                SOLUCIÓN INMEDIATA:
                1. Ve a: /admin/token/refresh
                2. Genera un nuevo refresh token
                3. Úsalo para actualizar SYSTEM_OAUTH2_REFRESH_TOKEN
                """);
        }

        return response;
    }
}