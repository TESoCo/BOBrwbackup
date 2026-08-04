package com.example.servicioWeb;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class GoogleOAuthService {

    @Autowired
    private EncryptionService encryptionService;

    // Estos valores vendrían de tu configuración de Google Cloud
    @Value("${google.client.id}")
    private String CLIENT_ID;

    @Value("${google.client.secret}")
    private String CLIENT_SECRET;

    public String getAccessToken(String encryptedRefreshToken) throws Exception {
        String refreshToken = encryptionService.decrypt(encryptedRefreshToken);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(
                new GoogleClientSecrets.Details().setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), JacksonFactory.getDefaultInstance(), clientSecrets,
                Collections.singletonList("https://www.googleapis.com/auth/gmail.send"))
                .build();

        // Refresca el token automáticamente usando el refresh_token
        return flow.newTokenRequest(refreshToken)
                .setGrantType("refresh_token")
                .execute()
                .getAccessToken();
    }

    /**
     * Verifica y decodifica el ID Token de Google
     */
    public GoogleIdToken verifyIdToken(String idTokenString) throws Exception {
        // Crear el verifier
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        // Verificar el token
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new RuntimeException("Token ID inválido o no verificado");
        }

        return idToken;
    }

    /**
     * Métod0 alternativo: Verifica el token y retorna el payload directamente
     */
    public GoogleIdToken.Payload verifyAndGetPayload(String idTokenString) throws Exception {
        GoogleIdToken idToken = verifyIdToken(idTokenString);
        return idToken.getPayload();
    }
}