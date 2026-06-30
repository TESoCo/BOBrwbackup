package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import com.example.servicioWeb.EncryptionService;
import com.example.servicioWeb.GoogleOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioServicioImpl implements UsuarioServicio {

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private GoogleOAuthService googleOAuthService;


    /**
     * OPERACIONES BASICAS
     */

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
     * BUSQUEDAS ESPECIALES
     */


    @Override
    @Transactional(readOnly = true)
    public Usuario encontrarPorId(Long id) {
        return usuarioDao.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario encontrarPorNombreUsuario(String nombreUsuario) {
        return usuarioDao.findByNombreUsuario(nombreUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> encontrarPorRol(String rol) {
        return usuarioDao.findByRol_NombreRol(rol);
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


    /**
     * LISTAR USUARIOS HUERFANOS
     */
    @Override
    @Transactional
    public List <Usuario> listarUsuariosSinEquipo(){
        List <Usuario> usuarioList = usuarioDao.findAll();
        List <Usuario> sinEquipo = new ArrayList<>();
        for (Usuario usuario : usuarioList){
            if (usuario.getEquipo()==null){
                sinEquipo.add(usuario);
            }
        }
        return sinEquipo;
    }

    /**
     * APROBACIÓN DE USUARIOS
     */
    @Override
    @Transactional
    public void aprobarUsuario(Long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow();
        usuario.setStatus("APPROVED");
        usuarioDao.save(usuario);
        // TODO Opcional: enviar email al usuario informando que ya puede entrar
    }

    /**
     * Obtiene un access token para un usuario OAuth2
     */
    @Override
    public String getAccessTokenForUser(Long userId) throws Exception {
        Usuario usuario = usuarioDao.findById(userId).orElseThrow();
        if (usuario == null || usuario.getGoogleRefreshToken() == null) {
            throw new RuntimeException("Usuario o refresh token no encontrado");
        }

        if ("GOOGLE".equals(usuario.getAuthProvider())) {
            return googleOAuthService.getAccessToken(usuario.getGoogleRefreshToken());
        }
        // Agregar otros proveedores aquí (Facebook, GitHub, etc.)

        throw new RuntimeException("Proveedor no soportado");
    }



    /**
     *  Encontrar usuario por correo
     */
    @Override
    @Transactional
    public List<Usuario> encontrarPorCorreo(String correo){

        return usuarioDao.findByPersona_Correo(correo);
    }



    /**
     * Métod0 auxiliar para descargar imagen desde URL
     */
    public byte[] downloadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            System.out.println("📥 Descargando imagen de: " + imageUrl);
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                     java.io.InputStream is = conn.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    byte[] result = baos.toByteArray();
                    System.out.println("Imagen descargada: " + result.length + " bytes");
                    return result;
                }
            } else {
                System.err.println("Error al descargar imagen, código: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println(" Error descargando imagen de Google: " + e.getMessage());
        }
        return null;
    }



}