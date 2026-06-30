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
            Usuario usuarioExistente = usuarioServicio.encontrarPorNombreUsuario(email);

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
                String status = usuarioExistente.getStatus();
                System.out.println("Estado del usuario: " + status);

                if ("PENDING".equals(status)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta está pendiente de aprobación por un administrador");
                    return "redirect:/login?pending=true";
                } else if ("REJECTED".equals(status)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Tu cuenta ha sido rechazada. Contacta al administrador.");
                    return "redirect:/login?rejected=true";
                } else if (!"APPROVED".equals(status)) {
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
     * Intercambia el código de autorización por tokens
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
     * Autentica al usuario manualmente
     */
    private void authenticateUser(Usuario usuario, HttpServletRequest request) {

        System.out.println("Autenticación");
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
}