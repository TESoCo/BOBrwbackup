package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Permiso;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioDetailsServices implements UserDetailsService {


    @Autowired
    private UsuarioServicio usuarioServicio;

//Method for loading a user by their name
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        if (usuario == null) {
            System.out.println("User not found: " + username);
            throw new UsernameNotFoundException("Usuario NO encontrado: " + username);
        }

        // Handle case where rol might be null
        String role = (usuario.getRol() != null && usuario.getRol().getNombreRol()!= null)
                ? usuario.getRol().getNombreRol().toUpperCase()
                : "USER";

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
        }
        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPass_usuario())
                .authorities(authorities) // Use authorities instead of roles()
                .build();
    }
}