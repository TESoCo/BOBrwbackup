package com.example.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class GmailOAuthInitializer {

    public static void main(String[] args) throws Exception {
        // Cargar credenciales desde el archivo credentials.json
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(
                        Files.newInputStream(Paths.get("credentials.json"))
                )
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singletonList(GmailScopes.GMAIL_SEND)
        ).setDataStoreFactory(new FileDataStoreFactory(new File("oauth-credentials")))
                .setAccessType("offline")
                .build();

        AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(
                flow,
                new LocalServerReceiver()
        );

        Credential credential = app.authorize("user");

        System.out.println("✅ Credenciales guardadas exitosamente en: " +
                new File("oauth-credentials").getAbsolutePath());
    }
}
