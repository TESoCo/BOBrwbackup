package com.example.controller.web;

import com.example.domain.Usuario;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.EncryptionService;
import com.example.servicioWeb.GoogleOAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/oauth2")
public class ControladorOAuth2 {

    @Autowired
    @Lazy
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuthenticationManager authenticationManager;



    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    // Valores para Microsoft
    @Value("${microsoft.client.id:}")
    private String microsoftClientId;

    @Value("${microsoft.client.secret:}")
    private String microsoftClientSecret;

    @Value("${microsoft.redirect.uri:}")
    private String microsoftRedirectUri;




    /**
     * Inicia el flujo de login con Google
     */
    @GetMapping("/login/google")
    public RedirectView loginWithGoogle() {
        try {
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                    .setInstalled(new GoogleClientSecrets.Details()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    clientSecrets,
                    Collections.singletonList(
                            "https://www.googleapis.com/auth/userinfo.email " +
                                    "https://www.googleapis.com/auth/userinfo.profile " +
                                    "openid " + "https://www.googleapis.com/auth/gmail.send")
            ).build();

            List<String> scopes = Arrays.asList(
                    "https://www.googleapis.com/auth/userinfo.email",
                    "https://www.googleapis.com/auth/userinfo.profile",
                    "https://www.googleapis.com/auth/gmail.send"  // ← ESTE ES EL CLAVE
            );

            String authorizationUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setResponseTypes(Collections.singleton("code"))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .setScopes(scopes)
                    .build();

            System.out.println("URL de autorización: " + authorizationUrl);
            return new RedirectView(authorizationUrl);

        } catch (Exception e) {
            System.err.println("Error al iniciar login con Google: " + e.getMessage());
            e.printStackTrace();
            return new RedirectView("/login?error=google_error");
        }
    }

    /**
     * Inicia el flujo de login con Microsoft
     */
    @GetMapping("/login/microsoft")
    public RedirectView loginWithMicrosoft() {
        try {
            // Construir URL de autorización de Microsoft
            String authorizationUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize" +
                    "?client_id=" + microsoftClientId +
                    "&response_type=code" +
                    "&redirect_uri=" + microsoftRedirectUri +
                    "&response_mode=query" +
                    "&scope=openid%20profile%20email%20User.Read" +
                    "&access_type=offline" +
                    "&prompt=consent";

            System.out.println("URL de autorización Microsoft: " + authorizationUrl);
            return new RedirectView(authorizationUrl);

        } catch (Exception e) {
            System.err.println("Error al iniciar login con Microsoft: " + e.getMessage());
            e.printStackTrace();
            return new RedirectView("/login?error=microsoft_error");
        }
    }

    /**
     * Callback de Google después de la autenticación
     */
    @GetMapping("/callback/google")
    public String googleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (error != null) {
            redirectAttributes.addFlashAttribute("error", "Error en autenticación con Google: " + error);
            return "redirect:/login";
        }

        if (code == null || code.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se recibió código de autorización");
            return "redirect:/login";
        }


        try {
            // 1. Intercambiar el código por un token de acceso
            GoogleTokenResponse tokenResponse = exchangeCodeForTokens(code);

            System.out.println("=== TOKEN RESPONSE ===");
            System.out.println("Access Token: " + tokenResponse.getAccessToken());
            System.out.println("Refresh Token: " + tokenResponse.getRefreshToken());
            System.out.println("Expires In: " + tokenResponse.getExpiresInSeconds());
            System.out.println("Token Type: " + tokenResponse.getTokenType());



            String idTokenString = tokenResponse.getIdToken();

            // 2. Verificar el ID Token
            GoogleIdToken idToken = googleOAuthService.verifyIdToken(idTokenString);
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String familyName = (String) payload.get("family_name");
            String givenName = (String) payload.get("given_name");
            String picture = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            System.out.println("=== LOGIN CON GOOGLE ===");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("Picture: " + picture);
            System.out.println("Email Verified: " + emailVerified);

            // 2a. Verificar que el usuario esté verificado en google.
            if (emailVerified == null || !emailVerified) {
                redirectAttributes.addFlashAttribute("error",
                        "El correo electrónico no está verificado en Google. Por favor, verifica tu cuenta.");
                return "redirect:/login";
            }

            // 3. Verificar si el usuario ya existe
            // Buscar por email en la tabla usuario o en persona
            List<Usuario> usuarios = usuarioServicio.encontrarPorCorreo(email);
            Usuario usuarioExistente = usuarios.isEmpty() ? null : usuarios.get(0);

            // Si no se encuentra por email, intentar por nombre de usuario como fallback
            if (usuarioExistente == null) {
                String possibleUsername = email.split("@")[0];
                usuarioExistente = usuarioServicio.encontrarPorNombreUsuario(possibleUsername);
            }

            if (usuarioExistente != null) {
                // USUARIO EXISTENTE: actulizar refresh token
                String refreshToken = tokenResponse.getRefreshToken();
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
                        usuarioExistente.setGoogleRefreshToken(encryptedRefreshToken);
                        usuarioServicio.guardar(usuarioExistente);
                    } catch (Exception e) {
                        System.err.println("No se pudo encriptar el refresh token: " + e.getMessage());
                    }
                }

                // Verificar el estado del usuario
                // Obtener el enum directamente
                Usuario.StatusUsuario status = usuarioExistente.getStatus();
                System.out.println("Estado del usuario: " + status);

                if (status == Usuario.StatusUsuario.PENDING) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta está pendiente de aprobación por un administrador");
                    return "redirect:/login?pending=true";
                } else if (status == Usuario.StatusUsuario.REJECTED) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta ha sido rechazada. Contacta al administrador.");
                    return "redirect:/login?rejected=true";
                } else if (status != Usuario.StatusUsuario.APPROVED) {
                    redirectAttributes.addFlashAttribute("error",
                            "Estado de cuenta no válido. Contacta al administrador.");
                    return "redirect:/login";
                }


                System.out.println("Autenticando usuario: " + usuarioExistente.getNombreUsuario());
                authenticateUser(usuarioExistente, request);
                System.out.println("Usuario autenticado correctamente");
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("Nombre: " + (auth != null ? auth.getName() : "null"));

                //Verificar sesión
                HttpSession session = request.getSession(false);
                if (session != null) {
                    System.out.println("   Session ID: " + session.getId());
                    Object contextAttr = session.getAttribute(
                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    System.out.println("   Contexto en sesión: " + (contextAttr != null ? "presente" : "ausente"));
                } else {
                    System.out.println("   ❌ No hay sesión activa");
                }

                redirectAttributes.addFlashAttribute("success", "¡Bienvenido de vuelta!");
                return "redirect:/dashboard";





            } else {
                // NUEVO USUARIO
                String refreshToken = tokenResponse.getRefreshToken();
                String encryptedRefreshToken = null;
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    encryptedRefreshToken = encryptionService.encrypt(refreshToken);
                }

                GoogleUserData googleData = new GoogleUserData(
                        email, name, givenName, familyName,
                        emailVerified, encryptedRefreshToken,
                        tokenResponse.getAccessToken(),
                        picture
                );

                redirectAttributes.addFlashAttribute("googleData", googleData);
                redirectAttributes.addFlashAttribute("success",
                        "¡Hola " + name + "! Completa el registro para terminar");

                return "redirect:/usuarios/registrar-google";
            }

        } catch (Exception e) {
            System.err.println("Error en callback de Google: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al autenticar con Google: " + e.getMessage());
            return "redirect:/login";
        }
    }


    /**
     * Callback de Microsoft después de la autenticación
     */
    @GetMapping("/callback/microsoft")
    public String microsoftCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (error != null) {
            redirectAttributes.addFlashAttribute("error", "Error en autenticación con Microsoft: " + error +
                    (errorDescription != null ? " - " + errorDescription : ""));
            return "redirect:/login";
        }

        if (code == null || code.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se recibió código de autorización");
            return "redirect:/login";
        }

        try {
            // Intercambiar el código por tokens
            MicrosoftTokenResponse tokenResponse = exchangeCodeForTokensMicrosoft(code);

            System.out.println("=== TOKEN RESPONSE MICROSOFT ===");
            System.out.println("Access Token: " + tokenResponse.accessToken);
            System.out.println("Refresh Token: " + tokenResponse.refreshToken);
            System.out.println("Expires In: " + tokenResponse.expiresIn);

            // Obtener información del usuario desde Microsoft Graph
            MicrosoftUserInfo userInfo = getUserInfoFromMicrosoft(tokenResponse.accessToken);

            String email = userInfo.mail != null ? userInfo.mail : userInfo.userPrincipalName;
            String name = userInfo.displayName;
            String givenName = userInfo.givenName;
            String familyName = userInfo.surname;
            String picture = null; // Microsoft no da foto en el user info

            System.out.println("=== LOGIN CON MICROSOFT ===");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("GivenName: " + givenName);
            System.out.println("FamilyName: " + familyName);

            if (email == null || email.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "No se pudo obtener el correo electrónico de Microsoft. Verifica tus permisos.");
                return "redirect:/login";
            }

            // Procesar el usuario (común para Google y Microsoft)
            return processOAuth2User(email, name, givenName, familyName, picture, true,
                    tokenResponse.refreshToken, "MICROSOFT", request, redirectAttributes);

        } catch (Exception e) {
            System.err.println("Error en callback de Microsoft: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al autenticar con Microsoft: " + e.getMessage());
            return "redirect:/login";
        }
    }


    /**
     * Métod común para procesar usuarios OAuth2 (tanto Google como Microsoft)
     */
    private String processOAuth2User(
            String email,
            String name,
            String givenName,
            String familyName,
            String picture,
            Boolean emailVerified,
            String refreshToken,
            String provider,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            // Verificar si el usuario ya existe por email
            Usuario usuarioExistente = null;
            if (email != null) {
                List<Usuario> usuarios = usuarioServicio.encontrarPorCorreo(email);
                if (!usuarios.isEmpty()) {
                    usuarioExistente = usuarios.get(0);
                }
            }

            // Si no se encontró por email, intentar por nombre de usuario
            if (usuarioExistente == null && email != null) {
                String username = email.split("@")[0];
                usuarioExistente = usuarioServicio.encontrarPorNombreUsuario(username);
            }

            if (usuarioExistente != null) {
                // USUARIO EXISTENTE: actualizar refresh token
                System.out.println("✅ Usuario encontrado: " + usuarioExistente.getNombreUsuario());

                String refreshTokenToStore = refreshToken;
                if (refreshTokenToStore != null && !refreshTokenToStore.isEmpty()) {
                    try {
                        String encryptedRefreshToken = encryptionService.encrypt(refreshTokenToStore);
                        usuarioExistente.setGoogleRefreshToken(encryptedRefreshToken);
                        usuarioServicio.guardar(usuarioExistente);
                        System.out.println("Refresh token actualizado para: " + usuarioExistente.getNombreUsuario());
                    } catch (Exception e) {
                        System.err.println("No se pudo encriptar el refresh token: " + e.getMessage());
                    }
                }

                // Verificar el estado del usuario
                Usuario.StatusUsuario status = usuarioExistente.getStatus();
                System.out.println("Estado del usuario: " + status);

                if (Usuario.StatusUsuario.PENDING.equals(status)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta está pendiente de aprobación por un administrador");
                    return "redirect:/login?pending=true";
                } else if (Usuario.StatusUsuario.REJECTED.equals(status)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta ha sido rechazada. Contacta al administrador.");
                    return "redirect:/login?rejected=true";
                } else if (!Usuario.StatusUsuario.APPROVED.equals(status)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Estado de cuenta no válido. Contacta al administrador.");
                    return "redirect:/login";
                }

                // Autenticar al usuario
                System.out.println("Autenticando usuario: " + usuarioExistente.getNombreUsuario());
                authenticateUser(usuarioExistente, request);
                System.out.println("Usuario autenticado correctamente");

                redirectAttributes.addFlashAttribute("success", "¡Bienvenido de vuelta!");
                return "redirect:/dashboard";

            } else {
                // NUEVO USUARIO
                System.out.println("🆕 Nuevo usuario de " + provider + ", redirigiendo a registro");

                String encryptedRefreshToken = null;
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        encryptedRefreshToken = encryptionService.encrypt(refreshToken);
                    } catch (Exception e) {
                        System.err.println("Error al encriptar refresh token: " + e.getMessage());
                    }
                }

                // Crear objeto de datos según el proveedor
                if ("GOOGLE".equals(provider)) {
                    GoogleUserData googleData = new GoogleUserData(
                            email, name, givenName, familyName,
                            emailVerified, encryptedRefreshToken, null, picture
                    );
                    redirectAttributes.addFlashAttribute("googleData", googleData);
                } else if ("MICROSOFT".equals(provider)) {
                    MicrosoftUserData microsoftData = new MicrosoftUserData(
                            email, name, givenName, familyName,
                            emailVerified, encryptedRefreshToken, picture
                    );
                    redirectAttributes.addFlashAttribute("microsoftData", microsoftData);
                }

                redirectAttributes.addFlashAttribute("success",
                        "¡Hola " + name + "! Completa el registro para terminar");

                return "redirect:/usuarios/registrar-" + provider.toLowerCase();
            }

        } catch (Exception e) {
            System.err.println("Error en processOAuth2User: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al procesar autenticación: " + e.getMessage());
            return "redirect:/login";
        }
    }




    /**
     * Intercambia el código de autorización por tokens (Google)
     */
    private GoogleTokenResponse exchangeCodeForTokens(String code) throws Exception {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singletonList(
                        "https://www.googleapis.com/auth/userinfo.email " +
                                "https://www.googleapis.com/auth/userinfo.profile " +
                                "openid")
        ).build();

        return flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();
    }


    /**
     * NUEVO: Intercambia el código de autorización por tokens (Microsoft)
     */
    private MicrosoftTokenResponse exchangeCodeForTokensMicrosoft(String code) throws Exception {
        // Construir la URL para el token exchange
        String tokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        // Crear el body de la petición
        String body = "client_id=" + microsoftClientId +
                "&client_secret=" + microsoftClientSecret +
                "&code=" + code +
                "&redirect_uri=" + microsoftRedirectUri +
                "&grant_type=authorization_code";

        // Realizar la petición HTTP
        java.net.URL url = new java.net.URL(tokenUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // Leer el error
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new Exception("Error en token exchange: " + responseCode + " - " + response.toString());
            }
        }
        // Leer la respuesta
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            // Parsear JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response.toString());

            MicrosoftTokenResponse tokenResponse = new MicrosoftTokenResponse();
            tokenResponse.accessToken = json.get("access_token").asText();
            tokenResponse.refreshToken = json.has("refresh_token") ? json.get("refresh_token").asText() : null;
            tokenResponse.expiresIn = json.get("expires_in").asInt();
            tokenResponse.tokenType = json.get("token_type").asText();

            return tokenResponse;
        }
    }


    /**
     * NUEVO: Obtiene información del usuario desde Microsoft Graph
     */
    private MicrosoftUserInfo getUserInfoFromMicrosoft(String accessToken) throws Exception {
        String userInfoUrl = "https://graph.microsoft.com/v1.0/me";

        java.net.URL url = new java.net.URL(userInfoUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Error al obtener información del usuario: " + responseCode);
        }

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response.toString());

            MicrosoftUserInfo userInfo = new MicrosoftUserInfo();
            userInfo.mail = json.has("mail") ? json.get("mail").asText() : null;
            userInfo.userPrincipalName = json.has("userPrincipalName") ? json.get("userPrincipalName").asText() : null;
            userInfo.displayName = json.has("displayName") ? json.get("displayName").asText() : null;
            userInfo.givenName = json.has("givenName") ? json.get("givenName").asText() : null;
            userInfo.surname = json.has("surname") ? json.get("surname").asText() : null;

            return userInfo;
        }
    }

    /**
     * Autentica al usuario manualmente (ambos lo usan)
     */
    private void authenticateUser(Usuario usuario, HttpServletRequest request) {

        System.out.println("Autenticación para usuario: " + usuario.getNombreUsuario());
        System.out.println("Rol del usuario: " + (usuario.getRol() != null ? usuario.getRol().getNombreRol() : "NULL"));

        // 1. Construir autoridades correctamente
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (usuario.getRol() != null && usuario.getRol().getNombreRol() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombreRol().toUpperCase()));

            // Permisos específicos del Rol
            if (usuario.getRol().getPermisoList() != null) {
                for (var permiso : usuario.getRol().getPermisoList()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getNombrePermiso()));
                    System.out.println("   Permiso agregado: " + permiso.getNombrePermiso());
                }
            }
        } else {
            System.err.println("ERROR: Usuario sin rol asignado: " + usuario.getNombreUsuario());
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            System.out.println(" Alerta!:  Rol por defecto: ROLE_USER");
        }

        System.out.println("   Authorities: " + authorities);

        // 2. Manejar contraseña para usuarios OAuth2
        String password = usuario.getPass_usuario();
        if (password == null || password.isEmpty()) {
            password = "oauth2_placeholder";
            System.out.println("          Usuario OAuth2 sin contraseña - usando placeholder");
        }

        // 3. Crear UserDetails
        UserDetails userDetails = User.builder()
                .username(usuario.getNombreUsuario())
                .password(password)
                .authorities(authorities)
                .build();

        // 4. Crear autenticación
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // 5. Establecer en el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("Autenticación establecida en SecurityContext");

        // 6. Guardar securityContext en la sesión
        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        System.out.println("Contexto guardado en la sesión");

        // 7. Verificar
        Object saved = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        System.out.println(" Verificación: " + (saved != null ? "PRESENTE" : "AUSENTE"));

    }


    /**
     * Clase auxiliar para transferir datos de Google
     */
    public static class GoogleUserData {
        public String email;
        public String name;
        public String givenName;
        public String familyName;
        public Boolean emailVerified;
        public String encryptedRefreshToken;
        public String accessToken;
        public String picture;

        // CONSTRUCTOR VACÍO (necesario para new GoogleUserData())
        public GoogleUserData() {
            // Constructor vacío para crear instancias sin parámetros
        }

        public GoogleUserData(String email, String name, String givenName, String familyName,
                              Boolean emailVerified, String encryptedRefreshToken, String accessToken, String picture) {
            this.email = email;
            this.name = name;
            this.givenName = givenName;
            this.familyName = familyName;
            this.emailVerified = emailVerified;
            this.encryptedRefreshToken = encryptedRefreshToken;
            this.accessToken = accessToken;
            this.picture = picture;
        }
    }

    /**
     * Clase auxiliar para datos de Microsoft
     */
    public static class MicrosoftUserData {
        public String email;
        public String name;
        public String givenName;
        public String familyName;
        public Boolean emailVerified;
        public String encryptedRefreshToken;
        public String picture;

        public MicrosoftUserData() {}

        public MicrosoftUserData(String email, String name, String givenName, String familyName,
                                 Boolean emailVerified, String encryptedRefreshToken, String picture) {
            this.email = email;
            this.name = name;
            this.givenName = givenName;
            this.familyName = familyName;
            this.emailVerified = emailVerified;
            this.encryptedRefreshToken = encryptedRefreshToken;
            this.picture = picture;
        }
    }

    /**
     * Clase para respuesta de tokens de Microsoft
     */
    private static class MicrosoftTokenResponse {
        public String accessToken;
        public String refreshToken;
        public int expiresIn;
        public String tokenType;
    }

    /**
     * Clase para información de usuario de Microsoft
     */
    private static class MicrosoftUserInfo {
        public String mail;
        public String userPrincipalName;
        public String displayName;
        public String givenName;
        public String surname;
    }
}