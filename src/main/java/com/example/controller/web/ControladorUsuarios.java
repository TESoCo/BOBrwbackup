package com.example.controller.web;

import com.example.domain.Persona;
import com.example.domain.Rol;
import com.example.domain.Usuario;
import com.example.servicio.PersonaServicio;
import com.example.servicio.RolServicio;
import com.example.servicio.UsuarioServicio;
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






    /**
     * Mostrar la página principal de gestión de usuarios con datos reales
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarGestionUsuarios(Model model) {
        try {
            // Obtener lista REAL de usuarios
            List<Usuario> usuarios = usuarioServicio.listarUsuarios();
            List<Rol> roles = rolServicio.listarRoles();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Agregar datos REALES al modelo
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("roles", roles);
            model.addAttribute("totalUsuarios", usuarios != null ? usuarios.size() : 0);
            model.addAttribute("usuarioActual", auth);

            // Estadísticas REALES por rol
            if (usuarios != null) {
                long adminCount = usuarios.stream()
                        .filter(u -> u.getRol() != null && "ADMIN".equalsIgnoreCase(u.getRol().getNombreRol()))
                        .count();
                long supervisorCount = usuarios.stream()
                        .filter(u -> u.getRol() != null && "SUPERVISOR".equalsIgnoreCase(u.getRol().getNombreRol()))
                        .count();
                long operativoCount = usuarios.stream()
                        .filter(u -> u.getRol() != null && "OPERATIVO".equalsIgnoreCase(u.getRol().getNombreRol()))
                        .count();

                model.addAttribute("adminCount", adminCount);
                model.addAttribute("supervisorCount", supervisorCount);
                model.addAttribute("operativoCount", operativoCount);
            } else {
                model.addAttribute("adminCount", 0);
                model.addAttribute("supervisorCount", 0);
                model.addAttribute("operativoCount", 0);
            }

        } catch (Exception e) {
            System.err.println("Error al cargar gestión de usuarios: " + e.getMessage());
            e.printStackTrace();
            // Agregar listas vacías en caso de error
            model.addAttribute("usuarios", List.of());
            model.addAttribute("roles", List.of());
            model.addAttribute("totalUsuarios", 0);
            model.addAttribute("adminCount", 0);
            model.addAttribute("supervisorCount", 0);
            model.addAttribute("operativoCount", 0);
        }

        return "usuarios/usuarios";
    }

    /**
     * Mostrar formulario de registro de usuario
     */
    @GetMapping("/registrar")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
            RedirectAttributes redirectAttributes) {

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
            usuario.setPass_usuario(passwordEncoder.encode(password));
            usuario.setCargo(cargo);
            usuario.setPersona(persona);
            usuario.setRol(rol);

            // 3. PROCESAR LA FOTO DE PERFIL (si se subió)
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

            redirectAttributes.addFlashAttribute("success", "Usuario registrado exitosamente");
            return "redirect:/usuarios?registroExitoso=true";

        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "redirect:/usuarios/registrar";
        }
    }

    /**
     * Eliminar usuario
     */
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
                personaServicio.borrar(persona);
                System.out.println("Usuario eliminado: " + id);
                usuarioServicio.borrar(usuario);
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

}
