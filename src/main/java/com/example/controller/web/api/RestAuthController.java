package com.example.controller.web.api;

import com.example.domain.Usuario;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowCredentials = "false") // Importante: allowCredentials
// Para Android + Web en desarrollo
//@CrossOrigin(origins = {"*", "http://localhost:3000"}, allowCredentials = "false") //<- esto funciona en Android?
//TODO: Versiones funcionales para desarrollo y producción de esta conexión para la app android
public class RestAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpSession session) {

        String username = credentials.get("username");
        String password = credentials.get("password");

        System.out.println("=== LOGIN REQUEST ===");
        System.out.println("Username: " + username);
        System.out.println("Password length: " + (password != null ? password.length() : 0));

        // Validar que se enviaron las credenciales
        Map<String, Object> response = new HashMap<>();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            response.put("success", false);
            response.put("error", "Usuario y contraseña son requeridos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // Buscar usuario por nombre de usuario
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
            if (usuario == null) {
                response.put("success", false);
                response.put("error", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            /*
            * VERSIÓN MÁS FLEXIBLE CON USUARIO O CORREO
            *
            * // Buscar primero por username, si no por email
                Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
                if (usuario == null && username.contains("@")) {
                usuario = usuarioServicio.encontrarPorEmail(username);
                }
            *
            * */

            // Autenticar usando Spring Security (esto crea la sesión)
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            Authentication authentication = authenticationManager.authenticate(authToken);

            // Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // También guardar en la sesión explícitamente
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Obtener UserDetails para información adicional
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Devolver información del usuario
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("userId", usuario.getIdUsuario());
            response.put("nombre", usuario.getPersona().getNombre());
            response.put("email", usuario.getPersona().getCorreo());
            response.put("username", usuario.getNombreUsuario());
            response.put("rol", usuario.getRol() != null ? usuario.getRol().getNombreRol() : "USER");
            response.put("sessionId", session.getId());  // Opcional: para debug

            System.out.println("Login exitoso para: " + username);
            System.out.println("Session ID: " + session.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Credenciales inválidas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();  // Destruir la sesión
        SecurityContextHolder.clearContext();  // Limpiar contexto

        return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Logout exitoso"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("authenticated", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            // Buscar por nombre de usuario (que es el authentication.getName()<- el usuario loggeado)
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(authentication.getName());

            if (usuario == null) {
                response.put("authenticated", false);
                response.put("error", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            response.put("authenticated", true);
            response.put("userId", usuario.getIdUsuario());
            response.put("nombre", usuario.getPersona().getNombre());
            response.put("email", usuario.getPersona().getCorreo());
            response.put("username", usuario.getNombreUsuario());
            response.put("rol", usuario.getRol() != null ? usuario.getRol().getNombreRol() : "USER");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}