package com.example.controller.web;

import com.example.domain.Persona;
import com.example.domain.Rol;
import com.example.domain.Usuario;
import com.example.servicio.PersonaServicio;
import com.example.servicio.RolServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/usuarios")
public class ControladorUsuarios {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private PersonaServicio personaServicio;

    @Autowired
    private RolServicio rolServicio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EncryptionService encryptionService;


    /**
     * Mostrar la página principal de gestión de usuarios con datos reales
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CREAR_USUARIO') or hasAuthority('EDITAR_USUARIO') or hasRole('ADMIN')")
    public String mostrarGestionUsuarios(Model model) {
        try {
            System.out.println("=== START mostrarGestionUsuarios ===");
            // Obtener lista REAL de usuarios
            List<Usuario> usuarios = usuarioServicio.listarUsuarios();
            System.out.println("Usuarios found: " + (usuarios != null ? usuarios.size() : 0));
            List<Rol> roles = rolServicio.listarRoles();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Agregar datos REALES al modelo
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("roles", roles);
            model.addAttribute("totalUsuarios", usuarios != null ? usuarios.size() : 0);
            model.addAttribute("usuarioActual", auth);

            // Estadísticas REALES por rol
            // Reemplaza desde "if (usuarios != null) {" hasta el final del "else { ... }"
            if (usuarios != null && roles != null) {
                // 1. Creamos la lista dinámica recorriendo todos los roles que existan en la BD
                List<java.util.Map<String, Object>> resumenRoles = roles.stream().map(rol -> {
                    // Contamos cuántos usuarios tienen asignado este rol específico
                    long count = usuarios.stream()
                            .filter(u -> u.getRol() != null && u.getRol().getIdRol().equals(rol.getIdRol()))
                            .count();

                    // Guardamos el nombre y la cantidad en un mapa temporal
                    java.util.Map<String, Object> info = new java.util.HashMap<>();
                    info.put("nombre", rol.getNombreRol());
                    info.put("cantidad", count);
                    return info;
                }).collect(java.util.stream.Collectors.toList());

                // 2. Enviamos la lista completa al HTML
                model.addAttribute("resumenRoles", resumenRoles);
            } else {
                model.addAttribute("resumenRoles", java.util.List.of());
            }

        } catch (Exception e) {
        System.err.println("Error al cargar gestión de usuarios: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("usuarios", java.util.List.of());
        model.addAttribute("roles", java.util.List.of());
        model.addAttribute("totalUsuarios", 0);
        model.addAttribute("resumenRoles", java.util.List.of()); // Nueva lista vacía
    }

        return "usuarios/usuarios";
    }

    /**
     * Mostrar formulario de registro de usuario
     */
    @GetMapping("/registrar")
    @PreAuthorize("hasAuthority('CREAR_USUARIO')")
    public String mostrarFormularioRegistro(Model model) {
        try {
            // Obtener lista REAL de roles disponibles
            List<Rol> roles = rolServicio.listarRoles();
            model.addAttribute("roles", roles);
        } catch (Exception e) {
            System.err.println("Error al cargar formulario de registro: " + e.getMessage());
            model.addAttribute("roles", List.of());
        }

        return "usuarios/registrarBOB";
    }

    /**
     * Procesar registro de nuevo usuario - CREA PERSONA Y USUARIO
     */
    @PostMapping("/registrar")
    @PreAuthorize("hasAuthority('CREAR_USUARIO')")
    public String registrarUsuario(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String telefono,
            @RequestParam String correo,
            @RequestParam String nombreUsuario,
            @RequestParam String password,
            @RequestParam String cargo,
            @RequestParam String rolSeleccionado,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            @RequestParam(value = "authProvider", defaultValue = "LOCAL") String authProvider,
            @RequestParam(value = "refreshToken", required = false) String refreshToken,
            RedirectAttributes redirectAttributes) {

        System.out.println("=== REGISTRO DE USUARIO ===");
        System.out.println("Auth Provider: " + authProvider);
        System.out.println("Foto recibida: " + (fotoPerfil != null ? fotoPerfil.getOriginalFilename() : "null"));
        System.out.println("Tamaño: " + (fotoPerfil != null ? fotoPerfil.getSize() : "0"));
        System.out.println("Tipo: " + (fotoPerfil != null ? fotoPerfil.getContentType() : "null"));


        try {
            // Validar campos obligatorios
            if (nombre == null || nombre.isEmpty() ||
                    apellido == null || apellido.isEmpty() ||
                    telefono == null || telefono.isEmpty() ||
                    correo == null || correo.isEmpty() ||
                    nombreUsuario == null || nombreUsuario.isEmpty() ||
                    password == null || password.isEmpty() ||
                    cargo == null || cargo.isEmpty() ||
                    rolSeleccionado == null || rolSeleccionado.isEmpty()) {

                redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios");
                return "redirect:/usuarios/registrar";
            }

            // Validar contraseña solo para registros LOCALES
            if ("LOCAL".equals(authProvider) && (password == null || password.isEmpty())) {
                redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria para registros locales");
                return "redirect:/usuarios/registrar";
            }

            // Verificar si el nombre de usuario ya existe
            Usuario usuarioExistente = usuarioServicio.encontrarPorNombreUsuario(nombreUsuario);
            if (usuarioExistente != null) {
                redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya existe");
                return "redirect:/usuarios/registrar";
            }

            // 1. CREAR LA NUEVA PERSONA
            Persona persona = new Persona();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setTelefono(telefono);
            persona.setCorreo(correo);

            // Guardar la persona en la base de datos
            personaServicio.salvar(persona);
            System.out.println("Persona creada con ID: " + persona.getIdPersona());

            // 2. BUSCAR EL ROL SELECCIONADO
            Rol rol = null;
            List<Rol> roles = rolServicio.listarRoles();
            for (Rol r : roles) {
                if (rolSeleccionado.equalsIgnoreCase(r.getNombreRol())) {
                    rol = r;
                    break;
                }
            }

            // Si no se encuentra el rol, usar uno por defecto
            if (rol == null) {
                // Buscar SUPER como fallback
                for (Rol r : roles) {
                    if ("SUPER".equalsIgnoreCase(r.getNombreRol())) {
                        rol = r;
                        break;
                    }
                }
                if (rol == null && !roles.isEmpty()) {
                    rol = roles.get(0); // Usar el primer rol disponible
                }
            }

            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "No se pudo asignar un rol válido");
                return "redirect:/usuarios/registrar";
            }

            // 3. CREAR EL NUEVO USUARIO
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario(nombreUsuario);
            //la contraseña se encripta y guarda más abajo
            usuario.setCargo(cargo);
            usuario.setPersona(persona);
            usuario.setRol(rol);


            //Forzar estado inicial (sin aprobación)
            usuario.setStatus("PENDING");

            // Establecer el proveedor de autenticación
            usuario.setAuthProvider(authProvider);

            // Encriptación condicional según el proveedor
            if ("LOCAL".equals(authProvider)) {
                // Para usuarios locales: encriptar la contraseña
                usuario.setPass_usuario(passwordEncoder.encode(password));
                System.out.println("Usuario LOCAL con contraseña encriptada");
            } else if ("GOOGLE".equals(authProvider) || "FACEBOOK".equals(authProvider)) {
                // Para OAuth2: Generar una contraseña aleatoria para usuarios OAuth2
                String randomPassword = java.util.UUID.randomUUID().toString();
                usuario.setPass_usuario(passwordEncoder.encode(randomPassword));
                System.out.println("Usuario OAuth2 con contraseña generada aleatoriamente");

                // Encriptar el refresh token si existe
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        // Encriptar el refresh token antes de guardarlo
                        String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
                        usuario.setGoogleRefreshToken(encryptedRefreshToken);
                        System.out.println("Refresh token encriptado guardado");
                    } catch (Exception e) {
                        System.err.println("Error al encriptar refresh token: " + e.getMessage());
                        redirectAttributes.addFlashAttribute("error", "Error al procesar token de autenticación");
                        return "redirect:/usuarios/registrar";
                    }
                }


            }



            // 4. PROCESAR LA FOTO DE PERFIL (si se subió)
            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                // Validar tipo de archivo
                String contentType = fotoPerfil.getContentType();
                if (!contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "El archivo debe ser una imagen");
                    return "redirect:/usuarios/registrar";
                }

                // Validar tamaño (máximo 5MB)
                if (fotoPerfil.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "La imagen debe ser menor a 5MB");
                    return "redirect:/usuarios/registrar";
                }

                // LEER LA FOTO COMO BYTES (BLOB)
                byte[] fotoBytes = fotoPerfil.getBytes();
                // GUARDAR COMO BLOB
                usuario.setFotoPerfil(fotoBytes);
                usuario.setFotoTipo(contentType); // Guardar el tipo MIME

                // Guardar usuario en la base de datos
                usuarioServicio.guardar(usuario);

            } else {
                // Usuario sin foto
                System.out.println("No se subió foto de perfil");
                usuarioServicio.guardar(usuario);
            }

            // Mensaje de éxito según el tipo de registro
            String mensajeExito;
            if ("LOCAL".equals(authProvider)) {
                mensajeExito = "¡Registro exitoso! Tu cuenta está a la espera de aprobación por un administrador.";
            } else {
                mensajeExito = "¡Registro con " + authProvider + " exitoso! Tu cuenta está a la espera de aprobación.";
            }

            redirectAttributes.addFlashAttribute("mensaje", mensajeExito);
            redirectAttributes.addFlashAttribute("success", "Usuario registrado exitosamente. Pendiente de aprobación.");
            return "redirect:/usuarios?registroExitoso=true";

        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "redirect:/usuarios/registrar";
        }
    }


    /**
     * Mostrar formulario de registro para usuarios de Google
     */
    @GetMapping("/registrar-google")
    public String mostrarFormularioRegistroGoogle(Model model, RedirectAttributes redirectAttributes) {
        try {
            // Verificar si hay datos de Google en la sesión
            if (!model.containsAttribute("googleData")) {
                // Intentar recuperar de flash attributes
                Object googleData = redirectAttributes.getFlashAttributes().get("googleData");
                if (googleData != null) {
                    model.addAttribute("googleData", googleData);
                } else {
                    // Si no hay datos, redirigir al login
                    redirectAttributes.addFlashAttribute("error",
                            "No se encontraron datos de autenticación. Por favor, intenta de nuevo.");
                    return "redirect:/login";
                }
            }

            // Obtener roles disponibles para el registro
            List<Rol> roles = rolServicio.listarRoles();
            model.addAttribute("roles", roles);

            return "usuarios/registrarGoogle";

        } catch (Exception e) {
            System.err.println("Error al cargar formulario de registro Google: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar el formulario");
            return "redirect:/login";
        }
    }


    /**
     * Procesar registro de usuario desde Google (sin contraseña)
     */
    @PostMapping("/registrar-google")
    public String registrarUsuarioGoogle(
            @RequestParam String nombreUsuario,
            @RequestParam String cargo,
            @RequestParam String rolSeleccionado,
            @RequestParam(required = false) String telefono,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            @ModelAttribute("googleData") ControladorOAuth2.GoogleUserData googleData,
            RedirectAttributes redirectAttributes) {

        try {
            if (googleData == null) {
                redirectAttributes.addFlashAttribute("error", "Datos de Google no encontrados");
                return "redirect:/login";
            }

            // Validar campos
            if (nombreUsuario == null || nombreUsuario.isEmpty() ||
                    cargo == null || cargo.isEmpty() ||
                    rolSeleccionado == null || rolSeleccionado.isEmpty()) {

                redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios");
                redirectAttributes.addFlashAttribute("googleData", googleData);
                return "redirect:/usuarios/registrar-google";
            }

            // Verificar si el nombre de usuario ya existe
            Usuario usuarioExistente = usuarioServicio.encontrarPorNombreUsuario(nombreUsuario);
            if (usuarioExistente != null) {
                redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya existe");
                redirectAttributes.addFlashAttribute("googleData", googleData);
                return "redirect:/usuarios/registrar-google";
            }

            // 1. CREAR PERSONA
            Persona persona = new Persona();

            // Verificar que los datos de Google no sean null
            String nombre = googleData.givenName != null ? googleData.givenName : "";
            if (nombre.isEmpty() && googleData.name != null) {
                // Si givenName está vacío pero name tiene valor, intentar separar
                String[] nameParts = googleData.name.split(" ");
                nombre = nameParts.length > 0 ? nameParts[0] : googleData.name;
            }

            String apellido = googleData.familyName != null ? googleData.familyName : "";
            if (apellido.isEmpty() && googleData.name != null && googleData.name.contains(" ")) {
                String[] nameParts = googleData.name.split(" ");
                if (nameParts.length > 1) {
                    apellido = nameParts[nameParts.length - 1];
                }
            }

            // Asignar valores por defecto como último recurso
            persona.setNombre(nombre.isEmpty() ? "Usuario" : nombre);
            persona.setApellido(apellido.isEmpty() ? "Google" : apellido);
            persona.setTelefono(telefono != null && !telefono.isEmpty() ? telefono : "0000000000");
            String email = googleData.email != null ? googleData.email : nombreUsuario + "@google.com";
            persona.setCorreo(email);


            personaServicio.salvar(persona);
            System.out.println("Persona creada con ID: " + persona.getIdPersona());

            // 2. BUSCAR ROL
            Rol rol = rolServicio.listarRoles().stream()
                    .filter(r -> r.getNombreRol().equalsIgnoreCase(rolSeleccionado))
                    .findFirst()
                    .orElse(null);

            if (rol == null){
                // Si no se encuentra, buscar rol por defecto
                rol = rolServicio.listarRoles().stream()
                        .filter(r -> "USER".equalsIgnoreCase(r.getNombreRol()) ||
                                "OPERATIVO".equalsIgnoreCase(r.getNombreRol()))
                        .findFirst()
                        .orElse(null);

                if (rol == null && !rolServicio.listarRoles().isEmpty()) {
                    rol = rolServicio.listarRoles().get(0);
                }
            }




            // 3. CREAR USUARIO
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario(nombreUsuario);
            String randomPassword = java.util.UUID.randomUUID().toString();
            usuario.setPass_usuario(passwordEncoder.encode(randomPassword)); // Contraseña al azar para OAuth2
            usuario.setCargo(cargo);
            usuario.setPersona(persona);
            usuario.setRol(rol);
            usuario.setStatus("PENDING");
            usuario.setAuthProvider("GOOGLE");
            usuario.setEmailVerified(googleData.emailVerified);

            // Guardar refresh token encriptado
            if (googleData.encryptedRefreshToken != null) {
                usuario.setGoogleRefreshToken(googleData.encryptedRefreshToken);
            }

            // 4. PROCESAR FOTO (opcional)
            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                String contentType = fotoPerfil.getContentType();
                if (contentType.startsWith("image/") && fotoPerfil.getSize() <= 5 * 1024 * 1024) {
                    usuario.setFotoPerfil(fotoPerfil.getBytes());
                    usuario.setFotoTipo(contentType);
                }
            }

            // Guardar usuario
            usuarioServicio.guardar(usuario);

            redirectAttributes.addFlashAttribute("success",
                    "¡Registro completado! Tu cuenta está pendiente de aprobación por un administrador.");
            return "redirect:/login?pending=true";

        } catch (Exception e) {
            System.err.println("Error al registrar usuario Google: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("googleData", googleData);
            return "redirect:/usuarios/registrar-google";
        }
    }



    /**
     * Aprobar un usuario pendiente
     */
    @PostMapping("/aprobar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO') or hasRole('ADMIN')")
    public String aprobarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioServicio.aprobarUsuario(id);
            redirectAttributes.addFlashAttribute("success", "Usuario aprobado exitosamente");
        } catch (Exception e) {
            System.err.println("Error al aprobar usuario: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al aprobar usuario: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    /**
     * Rechazar un usuario pendiente
     */
    @PostMapping("/rechazar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO') or hasRole('ADMIN')")
    public String rechazarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioServicio.encontrarPorId(id);
            if (usuario != null) {
                usuario.setStatus("REJECTED");
                usuarioServicio.guardar(usuario);
                redirectAttributes.addFlashAttribute("success", "Usuario rechazado");
            }
        } catch (Exception e) {
            System.err.println("Error al rechazar usuario: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al rechazar usuario: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }











    /**
     * Eliminar usuario
     */
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO')")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioServicio.encontrarPorId(id);
            if (usuario != null) {

// Verificar si el usuario actual está intentando eliminarse a sí mismo
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String currentUsername = auth.getName();

                if (usuario.getNombreUsuario().equals(currentUsername)) {
                    redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propio usuario");
                    return "redirect:/usuarios";
                }

                //  También eliminar la persona asociada
                Persona persona = usuario.getPersona();
                usuarioServicio.borrar(usuario);
                System.out.println("Usuario eliminado: " + id);
                if (persona != null) {
                    personaServicio.borrar(persona);
                    System.out.println("Persona eliminada: " + persona.getIdPersona());
                }
                redirectAttributes.addFlashAttribute("success", "Usuario eliminado exitosamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            e.printStackTrace();

            // Mensaje de error más específico
            if (e.getMessage().contains("constraint") || e.getMessage().contains("foreign key")) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el usuario porque tiene registros asociados en el sistema");
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al eliminar usuario: " + e.getMessage());
            }
        }
    return "redirect:/usuarios";

    }

    /**
     * Mostrar formulario de edición de usuario
     */
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        try {
            // Buscar el usuario por ID
            Usuario usuario = usuarioServicio.encontrarPorId(id);
            if(usuario==null){
                redirectAttributes.addFlashAttribute("error","Usuaro no encontrado");
                return  "redirect:/usuarios";
            }

            // Obtener lista REAL de roles disponibles
            List<Rol> roles = rolServicio.listarRoles();

            // Agregar datos al modelo
            model.addAttribute("usuario", usuario);
            model.addAttribute("roles", roles);
            model.addAttribute("persona", usuario.getPersona());

            return "usuarios/editarUsuario";

        } catch (Exception e) {
            System.err.println("Error al cargar formulario de edición: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar formulario de edición");
            return "redirect:/usuarios";
        }

    }


    /**
     * Procesar actualización de usuario
     */
    @PostMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO')")
    public String actualizarUsuario(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String telefono,
            @RequestParam String correo,
            @RequestParam String nombreUsuario,
            @RequestParam(required = false) String password, // Opcional para edición
            @RequestParam String cargo,
            @RequestParam String rolSeleccionado,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            @RequestParam(value = "eliminarFoto", defaultValue = "false") boolean eliminarFoto,
            RedirectAttributes redirectAttributes) {

        try {
            // Buscar el usuario existente
            Usuario usuarioExistente = usuarioServicio.encontrarPorId(id);
            if (usuarioExistente == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/usuarios";
            }

            // Validar campos obligatorios
            if (nombre == null || nombre.isEmpty() ||
                    apellido == null || apellido.isEmpty() ||
                    telefono == null || telefono.isEmpty() ||
                    correo == null || correo.isEmpty() ||
                    nombreUsuario == null || nombreUsuario.isEmpty() ||
                    cargo == null || cargo.isEmpty() ||
                    rolSeleccionado == null || rolSeleccionado.isEmpty()) {

                redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios");
                return "redirect:/usuarios/editar/" + id;
            }

            // Verificar si el nombre de usuario ya existe (excluyendo el usuario actual)
            Usuario usuarioConMismoNombre = usuarioServicio.encontrarPorNombreUsuario(nombreUsuario);
            if (usuarioConMismoNombre != null && !usuarioConMismoNombre.getIdUsuario().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya está en uso");
                return "redirect:/usuarios/editar/" + id;
            }

            // 1. ACTUALIZAR LA PERSONA
            Persona persona = usuarioExistente.getPersona();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setTelefono(telefono);
            persona.setCorreo(correo);
            personaServicio.salvar(persona);

            // 2. BUSCAR EL ROL SELECCIONADO
            Rol rol = null;
            List<Rol> roles = rolServicio.listarRoles();
            for (Rol r : roles) {
                if (rolSeleccionado.equalsIgnoreCase(r.getNombreRol())) {
                    rol = r;
                    break;
                }
            }

            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "No se pudo asignar un rol válido");
                return "redirect:/usuarios/editar/" + id;
            }

            // 3. ACTUALIZAR EL USUARIO
            usuarioExistente.setNombreUsuario(nombreUsuario);
            usuarioExistente.setCargo(cargo);
            usuarioExistente.setRol(rol);

            // Actualizar contraseña solo si se proporcionó una nueva
            if (password != null && !password.isEmpty()) {
                usuarioExistente.setPass_usuario(passwordEncoder.encode(password));
            }

            // 4. MANEJO DE LA FOTO DE PERFIL
            if (eliminarFoto) {
                // Eliminar foto existente
                usuarioExistente.setFotoPerfil(null);
                usuarioExistente.setFotoTipo(null);
            } else if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                // Validar tipo de archivo
                String contentType = fotoPerfil.getContentType();
                if (!contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "El archivo debe ser una imagen");
                    return "redirect:/usuarios/editar/" + id;
                }

                // Validar tamaño (máximo 5MB)
                if (fotoPerfil.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "La imagen debe ser menor a 5MB");
                    return "redirect:/usuarios/editar/" + id;
                }

                // Actualizar foto
                byte[] fotoBytes = fotoPerfil.getBytes();
                usuarioExistente.setFotoPerfil(fotoBytes);
                usuarioExistente.setFotoTipo(contentType);
            }
            // Si no se elimina ni se sube nueva foto, se mantiene la existente

            // Guardar cambios
            usuarioServicio.guardar(usuarioExistente);

            redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
            return "redirect:/usuarios?actualizacionExitosa=true";

        } catch (Exception e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
    }




        //nodo que sirve las fotos
    @GetMapping("/foto/{idUsuario}")
    @ResponseBody
    public ResponseEntity<byte[]> obtenerFotoUsuario(@PathVariable Long idUsuario) {
        try {
            Usuario usuario = usuarioServicio.encontrarPorId(idUsuario);
            if (usuario != null && usuario.getFotoPerfil() != null && usuario.getFotoTipo() != null) {
                HttpHeaders headers = new HttpHeaders();

                // Configurar el tipo de contenido basado en lo guardado
                if (usuario.getFotoTipo().equals("image/jpeg")) {
                    headers.setContentType(MediaType.IMAGE_JPEG);
                } else if (usuario.getFotoTipo().equals("image/png")) {
                    headers.setContentType(MediaType.IMAGE_PNG);
                } else if (usuario.getFotoTipo().equals("image/gif")) {
                    headers.setContentType(MediaType.IMAGE_GIF);
                } else {
                    headers.setContentType(MediaType.IMAGE_JPEG); // Por defecto
                }

                return new ResponseEntity<>(usuario.getFotoPerfil(), headers, HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            System.err.println("Error al obtener foto: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Usuario> ListarUsuariosPorRol(Rol rol){
        return usuarioServicio.encontrarPorRol(rol.getNombreRol());
    }

    /**
     * Filtrar usuarios por rol
     */
    @GetMapping("/filtrar")
    @PreAuthorize("hasAuthority('CREAR_USUARIO') or hasAuthority('EDITAR_USUARIO') or hasRole('ADMIN')")
    public String filtrarUsuariosPorRol(
            @RequestParam(name = "rol", defaultValue = "todos") String rol,
            Model model) {

        try {
            System.out.println("=== START filtrarUsuariosPorRol ===");
            System.out.println("Rol seleccionado: " + rol);

            // Obtener todos los usuarios
            List<Usuario> todosUsuarios = usuarioServicio.listarUsuarios();
            List<Rol> roles = rolServicio.listarRoles();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Filtrar usuarios por rol seleccionado
            // Obtener usuarios filtrados
            List<Usuario> usuariosFiltrados;

            if (rol == null || "todos".equalsIgnoreCase(rol)) {
                usuariosFiltrados = todosUsuarios;
            } else {
                // Usar el  existente que ya filtra por rol
                usuariosFiltrados = ListarUsuariosPorRol(rolServicio.buscarPorNombre(rol).get(0));
            }
            System.out.println("Usuarios filtrados: " + usuariosFiltrados.size());

            // Agregar datos al modelo
            model.addAttribute("usuarios", todosUsuarios);
            model.addAttribute("filteredUsers", usuariosFiltrados);
            model.addAttribute("selectedRol", rol);
            model.addAttribute("roles", roles);
            model.addAttribute("totalUsuarios", todosUsuarios != null ? todosUsuarios.size() : 0);
            model.addAttribute("usuarioActual", auth);

            // Estadísticas por rol (mismo código que en mostrarGestionUsuarios)
            if (todosUsuarios != null && roles != null) {
                List<java.util.Map<String, Object>> resumenRoles = roles.stream().map(r -> {
                    long count = todosUsuarios.stream()
                            .filter(u -> u.getRol() != null && u.getRol().getIdRol().equals(r.getIdRol()))
                            .count();

                    java.util.Map<String, Object> info = new java.util.HashMap<>();
                    info.put("nombre", r.getNombreRol());
                    info.put("cantidad", count);
                    return info;
                }).collect(java.util.stream.Collectors.toList());

                model.addAttribute("resumenRoles", resumenRoles);
            } else {
                model.addAttribute("resumenRoles", java.util.List.of());
            }

        } catch (Exception e) {
            System.err.println("Error al filtrar usuarios: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("usuarios", java.util.List.of());
            model.addAttribute("filteredUsers", java.util.List.of());
            model.addAttribute("roles", java.util.List.of());
            model.addAttribute("totalUsuarios", 0);
            model.addAttribute("resumenRoles", java.util.List.of());
            model.addAttribute("selectedRol", rol);
        }

        return "usuarios/usuarios";
    }
}
