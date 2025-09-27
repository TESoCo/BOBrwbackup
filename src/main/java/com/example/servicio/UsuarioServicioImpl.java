package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServicioImpl implements UsuarioServicio {

    @Autowired
    private UsuarioDao usuarioDao;

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return (List<Usuario>) usuarioDao.findAll();
    }

    @Override
    @Transactional
    public void guardar(Usuario usuario) {
        usuarioDao.save(usuario);
    }

    @Override
    @Transactional
    public void borrar(Usuario usuario) {
        usuarioDao.delete(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario encontrarPorId(Long id) {
        return usuarioDao.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario encontrarPorNombreUsuario(String nombreUsuario) {
        return usuarioDao.findBynombreUsuario(nombreUsuario);
    }

    @Override
    @Transactional
    public List <Usuario> buscarPorNombreList(String nombreUsuario)
    {
        return usuarioDao.findByNombreUsuario(nombreUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> encontrarPorRol(String rol) {
        return usuarioDao.findByRol_NombreRol(rol);
    }

    @Override
    @Transactional
    public void eliminarUsuarioConValidaciones(Long idUsuario) {
        Usuario usuario = encontrarPorId(idUsuario);
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }
        // Verificar si es el usuario actual
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (usuario.getNombreUsuario().equals(auth.getName())) {
            throw new RuntimeException("No puedes eliminar tu propio usuario");
        }
        // Aquí puedes agregar más validaciones de dependencias

        borrar(usuario);

        // Opcional: eliminar la persona asociada
        if (usuario.getPersona() != null) {
            // personaServicio.borrar(usuario.getPersona());
        }
    }

    /**
     * Verificar si el usuario tiene foto
     */
    @Override
    @Transactional
    public boolean tieneFoto(Long idUsuario) {
        Usuario usuario = encontrarPorId(idUsuario);
        return usuario != null && usuario.getFotoPerfil() != null && usuario.getFotoPerfil().length > 0;
    }








}