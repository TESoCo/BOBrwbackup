package com.example.servicio;

import com.example.domain.Usuario;
import java.util.List;

public interface UsuarioServicio {
    public List<Usuario> listarUsuarios();
    public void guardar(Usuario usuario);
    public void borrar(Usuario usuario);
    public Usuario encontrarPorId(Long id);
    Usuario encontrarPorNombreUsuario(String nombreUsuario);
    List<Usuario> encontrarPorRol(String rol);
}