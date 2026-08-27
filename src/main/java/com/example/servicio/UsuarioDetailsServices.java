package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Permiso;
import com.example.domain.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class UsuarioDetailsServices implements UserDetailsService {


    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private HttpSession session;

    //Method for loading a user by their name
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        //¿el usuario existe?
        if (usuario == null) {
            System.out.println("User not found: " + username);
            throw new UsernameNotFoundException("Usuario NO encontrado: " + username);
        }


        // Guardar estado en sesión ANTES de lanzar excepción
        if (Usuario.StatusUsuario.PENDING.equals(usuario.getStatus())) {
            session.setAttribute("loginError", "pending");
            throw new DisabledException("Cuenta pendiente de aprobación administrativa.");
        } else if (Usuario.StatusUsuario.REJECTED.equals(usuario.getStatus())) {
            session.setAttribute("loginError", "rejected");
            throw new DisabledException("Cuenta rechazada por el administrador.");
        } else if (!Usuario.StatusUsuario.APPROVED.equals(usuario.getStatus())) {
            session.setAttribute("loginError", "invalid");
            throw new DisabledException("Estado de cuenta no válido.");
        }



        // Get permissions and convert to Spring Security authorities
        // Crear autoridades
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role authority
        // 1. Agregar el rol como autoridad con prefijo ROLE_
        if (usuario.getRol() != null && usuario.getRol().getNombreRol() != null) {
            String roleName = usuario.getRol().getNombreRol();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));

        // 2. Agregar permisos específicos
        // Add permissions from the role
            if (usuario.getRol().getPermisoList() != null) {
                for (Permiso permiso : usuario.getRol().getPermisoList()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getNombrePermiso()));
                }
            }
        } else {
            // Rol por defecto si no tiene
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Manejar el caso donde pass_usuario puede ser null (usuarios OAuth2)
        String password = usuario.getPass_usuario();
        if (password == null) {
            // Para usuarios OAuth2, usar un placeholder
            password = "oauth2_placeholder";
            System.out.println(" Usuario OAuth2 sin contraseña en loadUserByUsername");
        }



        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(password)
                .authorities(authorities) // Use authorities instead of roles()
                .build();
    }

}