package com.example.controller.web;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ControladorPerfil {
    private final UsuarioDao usuarioDao;
    public ControladorPerfil(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Authentication auth, Model model) {
        String username = auth.getName();
        Usuario usuario = usuarioDao.findBynombreUsuario(username);
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @GetMapping("/perfil/foto")
    @ResponseBody
    public ResponseEntity<byte[]> mostrarFoto(Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioDao.findBynombreUsuario(username);

        if (usuario == null || usuario.getFotoPerfil() == null) {
            return ResponseEntity.notFound().build();
        }

        // Tipo MIME que guardaste en 'fotoTipo' (por ejemplo "image/jpeg")
        String tipo = usuario.getFotoTipo() != null ? usuario.getFotoTipo() : "image/jpeg";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, tipo)
                .body(usuario.getFotoPerfil());
    }
}