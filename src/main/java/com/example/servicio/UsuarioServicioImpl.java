package com.example.servicio;

import com.example.dao.UsuarioDao;
import com.example.domain.Usuario;
import com.example.servicioWeb.EmailResult;
import com.example.servicioWeb.EmailService;
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

    @Autowired
    private EmailService emailService;


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
        // ✅ ENVIAR CORREO DE NOTIFICACIÓN
        enviarCorreoAprobacion(usuario);
    }

    /**
     * Envía correo de notificación de aprobación usando la cuenta central
     */
    private void enviarCorreoAprobacion(Usuario usuario) {
        try {
            // 1. Verificar que el usuario tenga email
            if (usuario.getPersona() == null || usuario.getPersona().getCorreo() == null) {
                System.err.println("⚠️ Usuario sin email: " + usuario.getNombreUsuario());
                return;
            }

            String correoDestino = usuario.getPersona().getCorreo();

            // 2. Construir el contenido del correo
            String asunto = "🎉 ¡Tu cuenta ha sido aprobada! - Sistema COB";
            String contenido = construirContenidoEmail(usuario);

            // 3. Enviar usando el EmailService (cuenta central del sistema)
            List<String> destinatarios = List.of(correoDestino);

            System.out.println("📧 Enviando correo de aprobación a: " + correoDestino);

            EmailService.EmailResult resultado = emailService.sendMassEmailWithAttachment(
                    destinatarios,
                    asunto,
                    contenido,
                    null,           // Sin adjunto
                    null,           // Sin nombre de adjunto
                    "SISTEMA"       // Usuario que envía (para logging)
            );

            // 4. Verificar resultado
            if (resultado.getSuccessCount() > 0) {
                System.out.println("✅ Correo de aprobación enviado exitosamente a: " + correoDestino);
            } else {
                System.err.println("❌ Falló el envío de correo a: " + correoDestino);
                if (!resultado.getFailedEmails().isEmpty()) {
                    System.err.println("   Falló para: " + resultado.getFailedEmails());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo de aprobación para usuario " +
                    usuario.getNombreUsuario() + ": " + e.getMessage());
            e.printStackTrace();
            // No lanzamos excepción para no interrumpir el flujo de aprobación
        }
    }

    /**
     * Construye el contenido HTML del correo de aprobación
     */
    private String construirContenidoEmail(Usuario usuario) {
        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
        String nombreUsuario = usuario.getNombreUsuario();
        String correo = usuario.getPersona().getCorreo();
        String rol = usuario.getRol() != null ? usuario.getRol().getNombreRol() : "Usuario";
        String cargo = usuario.getCargo() != null ? usuario.getCargo() : "";

        return String.format(
                """
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 0; border: 1px solid #ddd; border-radius: 10px; overflow: hidden; }
                        .header { background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .header p { margin: 10px 0 0; opacity: 0.9; }
                        .content { padding: 30px; background: #f9f9f9; }
                        .info-box { background: white; padding: 20px; border-radius: 8px; margin: 15px 0; border: 1px solid #e0e0e0; }
                        .info-item { display: flex; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
                        .info-item:last-child { border-bottom: none; }
                        .info-label { font-weight: 600; width: 120px; color: #555; }
                        .info-value { flex: 1; }
                        .button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #4CAF50, #45a049); 
                                 color: white; text-decoration: none; border-radius: 5px; margin-top: 15px; font-weight: 600; }
                        .button:hover { background: linear-gradient(135deg, #45a049, #3d8b40); }
                        .footer { background: #f5f5f5; padding: 20px; text-align: center; color: #888; font-size: 12px; border-top: 1px solid #ddd; }
                        .footer a { color: #4CAF50; text-decoration: none; }
                        .success-icon { font-size: 48px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="success-icon"></div>
                            <h1>¡Cuenta Aprobada!</h1>
                            <p>Bienvenido al Sistema COB</p>
                        </div>
                        <div class="content">
                            <p>Estimado(a) <strong>%s</strong>,</p>
                            <p>Nos complace informarte que tu cuenta ha sido <strong>aprobada</strong> exitosamente.</p>
                            
                            <div class="info-box">
                                <div class="info-item">
                                    <span class="info-label">👤 Usuario:</span>
                                    <span class="info-value"><strong>%s</strong></span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label"> Email:</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label"> Rol:</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label"> Cargo:</span>
                                    <span class="info-value">%s</span>
                                </div>
                            </div>
                            
                            
                            
                            <p style="font-size: 14px; color: #666; margin-top: 20px;">
                                <strong> Nota:</strong> Puedes acceder con tu nombre de usuario y la contraseña que registraste, si te registraste con Google o Microsoft, no es necesaria una contraseña.
                                Si olvidaste tu contraseña, utiliza la opción "Recuperar contraseña" en la pantalla de inicio.
                            </p>
                        </div>
                        <div class="footer">
                            <p>Este mensaje fue enviado automáticamente por el Sistema BOB.</p>
                            <p>© 2024 Sistema COB - Todos los derechos reservados</p>
                            <p><small>Si tienes preguntas, contacta al administrador del sistema.</small></p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                nombreCompleto,
                nombreUsuario,
                correo,
                rol,
                cargo
        );
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