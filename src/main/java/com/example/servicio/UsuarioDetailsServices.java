package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServices implements UserDetailsService {

    @Autowired
    private UsuarioDao usuarioDao;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Usuario usuario = usuarioDao.findBynombreUsuario(username);

        if (usuario == null){
            throw new UsernameNotFoundException("Usuario NO encontrado");
        }

        // Handle case where rol might be null
        String role = (usuario.getRol() != null && usuario.getRol().getDescripRol() != null)
                ? usuario.getRol().getDescripRol().toUpperCase()
                : "USER";

        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPass_usuario())
                .roles(role)
                .build();
    }

}