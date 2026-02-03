package com.example.controller.web;



import com.example.domain.Rol;
import com.example.domain.Permiso;
import com.example.servicio.RolServicio;
import com.example.servicio.PermisoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/roles")
@PreAuthorize("hasAnyAuthority('CREAR_ROL', 'EDITAR_ROL')")
public class ControladorRoles {

    @Autowired
    private RolServicio rolServicio;

    @Autowired
    private PermisoServicio permisoServicio;

    // 1. LISTAR TODOS LOS ROLES
    @GetMapping
    public String listarRoles(Model model) {
        List<Rol> roles = rolServicio.listarRoles();
        model.addAttribute("roles", roles);
        return "roles/lista"; //TODO: Crear pantallas de control de roles y permisos
    }


    // 2. FORMULARIO PARA CREAR NUEVO ROL
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('CREAR_ROL')")
    public String mostrarFormularioNuevoRol(Model model) {
        Rol rol = new Rol();
        List<Permiso> todosPermisos = permisoServicio.listarPermisos();

        model.addAttribute("rol", rol);
        model.addAttribute("todosPermisos", todosPermisos);
        model.addAttribute("modo", "crear");
        return "roles/formulario";
    }


    // 3. GUARDAR NUEVO ROL
    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('CREAR_ROL')")
    public String guardarRol(
            @RequestParam String nombreRol,
            @RequestParam String descripRol,
            @RequestParam(required = false) List<Long> permisosIds,
            RedirectAttributes redirectAttributes) {

        try {
            // Verificar si el rol ya existe
            List<Rol> rolesExistentes = rolServicio.buscarPorNombre(nombreRol);
            if (!rolesExistentes.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un rol con ese nombre");
                return "redirect:/roles/nuevo";
            }

            // Crear nuevo rol
            Rol rol = new Rol();
            rol.setNombreRol(nombreRol);
            rol.setDescripRol(descripRol);

            // Asignar permisos seleccionados
            if (permisosIds != null && !permisosIds.isEmpty()) {
                List<Permiso> permisosSeleccionados = permisoServicio.buscarPorIds(permisosIds);
                rol.setPermisoList(permisosSeleccionados);
            }

            rolServicio.guardar(rol);
            redirectAttributes.addFlashAttribute("success", "Rol creado exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear rol: " + e.getMessage());
            return "redirect:/roles/nuevo";
        }

        return "redirect:/roles";
    }


    // 4. FORMULARIO PARA EDITAR ROL
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Rol rol = rolServicio.buscarPorId(id);
            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "Rol no encontrado");
                return "redirect:/roles";
            }

            List<Permiso> todosPermisos = permisoServicio.listarPermisos();

            model.addAttribute("rol", rol);
            model.addAttribute("todosPermisos", todosPermisos);
            model.addAttribute("modo", "editar");

            return "roles/formulario";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar rol: " + e.getMessage());
            return "redirect:/roles";
        }
    }


    // 5. ACTUALIZAR ROL
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    public String actualizarRol(
            @PathVariable Long id,
            @RequestParam String nombreRol,
            @RequestParam String descripRol,
            @RequestParam(required = false) List<Long> permisosIds,
            RedirectAttributes redirectAttributes) {

        try {
            Rol rol = rolServicio.buscarPorId(id);
            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "Rol no encontrado");
                return "redirect:/roles";
            }

            // Verificar si el nombre ya existe (excluyendo el actual)
            if (!rol.getNombreRol().equals(nombreRol)) {
                List<Rol> rolesExistentes = rolServicio.buscarPorNombre(nombreRol);
                if (!rolesExistentes.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe otro rol con ese nombre");
                    return "redirect:/roles/editar/" + id;
                }
            }

            rol.setNombreRol(nombreRol);
            rol.setDescripRol(descripRol);

            // Actualizar permisos
            if (permisosIds != null && !permisosIds.isEmpty()) {
                List<Permiso> permisosSeleccionados = permisoServicio.buscarPorIds(permisosIds);
                rol.setPermisoList(permisosSeleccionados);
            } else {
                rol.setPermisoList(List.of()); // Vaciar permisos si no se seleccionaron
            }

            rolServicio.guardar(rol);
            redirectAttributes.addFlashAttribute("success", "Rol actualizado exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar rol: " + e.getMessage());
            return "redirect:/roles/editar/" + id;
        }

        return "redirect:/roles";
    }

    // 6. ELIMINAR ROL (con validaciones)
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    public String eliminarRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Rol rol = rolServicio.buscarPorId(id);
            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "Rol no encontrado");
                return "redirect:/roles";
            }

            // Verificar si hay usuarios con este rol
            if (rol.getUsuarios() != null && !rol.getUsuarios().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "No se puede eliminar el rol porque tiene " + rol.getUsuarios().size() +
                                " usuario(s) asignado(s). Reasigne los usuarios primero.");
                return "redirect:/roles";
            }

            // No permitir eliminar roles básicos
            if ("ADMIN".equalsIgnoreCase(rol.getNombreRol()) ||
                    "SUPERVISOR".equalsIgnoreCase(rol.getNombreRol()) ||
                    "OPERATIVO".equalsIgnoreCase(rol.getNombreRol())) {
                redirectAttributes.addFlashAttribute("error",
                        "No se pueden eliminar los roles básicos del sistema (ADMIN, SUPERVISOR, OPERATIVO)");
                return "redirect:/roles";
            }

            rolServicio.eliminar(rol);
            redirectAttributes.addFlashAttribute("success", "Rol eliminado exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar rol: " + e.getMessage());
        }

        return "redirect:/roles";
    }



    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    public String verPermisos(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes){

        try {
            Rol rol = rolServicio.buscarPorId(id);
                if (rol == null) {
                    redirectAttributes.addFlashAttribute("error", "Rol no encontrado");
                    return "redirect:/roles";
                }

            List<Permiso> todos = permisoServicio.listarPermisos();

            model.addAttribute("rol", rol);
            model.addAttribute("todosPermisos", todos);
            return "roles/asignar_permisos";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar permisos: " + e.getMessage());
            return "redirect:/roles";
        }

    }

    @PostMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    public String actualizarPermisos(@PathVariable Long id,
                                     @RequestParam(required = false) List<Long> permisosIds,RedirectAttributes redirectAttributes){

        try {
            Rol rol = rolServicio.buscarPorId(id);
            if (rol == null) {
                redirectAttributes.addFlashAttribute("error", "Rol no encontrado");
                return "redirect:/roles";
            }

            List<Permiso> permisosSeleccionados = List.of();
            if (permisosIds != null && !permisosIds.isEmpty()) {
                permisosSeleccionados = permisoServicio.listarPermisos()
                        .stream()
                        .filter(p -> permisosIds.contains(p.getIdPermiso()))
                        .toList();
            }


            rol.setPermisoList(permisosSeleccionados);
            rolServicio.guardar(rol);

            redirectAttributes.addFlashAttribute("success", "Permisos actualizados exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar permisos: " + e.getMessage());
            return "redirect:/roles/" + id + "/permisos";

        }

        return "redirect:/roles";

    }
}
