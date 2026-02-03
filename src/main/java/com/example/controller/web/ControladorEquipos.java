package com.example.controller.web;

import com.example.domain.Equipo;
import com.example.domain.Usuario;
import com.example.domain.Proyecto;
import com.example.servicio.EquipoServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicio.ProyectoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/equipos")
public class ControladorEquipos {

    @Autowired
    private EquipoServicio equipoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;

    /**
     * Mostrar la lista de equipos
     */
    @GetMapping
    @PreAuthorize("hasAuthority('GESTIONAR_EQUIPOS') or hasAuthority('LEER_EQUIPO')")
    public String listarEquipos(Model model) {
        try {
            List<Equipo> equipos = equipoServicio.listarEquipos();
            model.addAttribute("equipos", equipos);
            model.addAttribute("totalEquipos", equipos != null ? equipos.size() : 0);

            // Estadísticas
            if (equipos != null) {
                long equiposConUsuarios = equipos.stream()
                        .filter(e -> e.getUsuarios() != null && !e.getUsuarios().isEmpty())
                        .count();
                long equiposConProyectos = equipos.stream()
                        .filter(e -> e.getProyectos() != null && !e.getProyectos().isEmpty())
                        .count();
                long equiposVacios = equipos.stream()
                        .filter(e -> (e.getUsuarios() == null || e.getUsuarios().isEmpty()) &&
                                (e.getProyectos() == null || e.getProyectos().isEmpty()))
                        .count();

                model.addAttribute("equiposConUsuarios", equiposConUsuarios);
                model.addAttribute("equiposConProyectos", equiposConProyectos);
                model.addAttribute("equiposVacios", equiposVacios);
            }

        } catch (Exception e) {
            System.err.println("Error al listar equipos: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("equipos", List.of());
            model.addAttribute("totalEquipos", 0);
        }

        return "equipos/equipos_lista";
    }

    /**
     * Mostrar formulario para crear nuevo equipo
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('CREAR_EQUIPO')")
    public String mostrarFormularioNuevo(Model model) {
        try {
            model.addAttribute("modo", "crear");
            model.addAttribute("equipo", new Equipo());
        } catch (Exception e) {
            System.err.println("Error al cargar formulario nuevo equipo: " + e.getMessage());
            e.printStackTrace();
        }

        return "equipos/equipo_formulario";
    }

    /**
     * Guardar nuevo equipo
     */
    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('CREAR_EQUIPO')")
    public String guardarEquipo(
            @ModelAttribute Equipo equipo,
            RedirectAttributes redirectAttributes) {

        try {
            // Validar campos obligatorios
            if (equipo.getDescEquipo() == null || equipo.getDescEquipo().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La descripción del equipo es obligatoria");
                return "redirect:/equipos/nuevo";
            }

            equipoServicio.guardar(equipo);
            redirectAttributes.addFlashAttribute("success", "Equipo creado exitosamente");
            return "redirect:/equipos?creacionExitosa=true";

        } catch (Exception e) {
            System.err.println("Error al guardar equipo: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al crear equipo: " + e.getMessage());
            return "redirect:/equipos/nuevo";
        }
    }

    /**
     * Mostrar detalles de un equipo
     */
    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAuthority('LEER_EQUIPO')")
    public String mostrarDetalleEquipo(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Equipo equipo = equipoServicio.encontrarPorId(id);
            if (equipo == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo no encontrado");
                return "redirect:/equipos";
            }

            model.addAttribute("equipo", equipo);
            return "equipos/equipo_detalle";

        } catch (Exception e) {
            System.err.println("Error al cargar detalle del equipo: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar equipo");
            return "redirect:/equipos";
        }
    }

    /**
     * Mostrar formulario para editar equipo
     */
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_EQUIPO')")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Equipo equipo = equipoServicio.encontrarPorId(id);
            if (equipo == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo no encontrado");
                return "redirect:/equipos";
            }

            model.addAttribute("equipo", equipo);
            model.addAttribute("modo", "editar");

            return "equipos/equipo_formulario";

        } catch (Exception e) {
            System.err.println("Error al cargar formulario de edición: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar formulario de edición");
            return "redirect:/equipos";
        }
    }

    /**
     * Actualizar equipo existente
     */
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_EQUIPO')")
    public String actualizarEquipo(
            @PathVariable Long id,
            @ModelAttribute Equipo equipo,
            RedirectAttributes redirectAttributes) {

        try {
            // Buscar equipo existente
            Equipo equipoExistente = equipoServicio.encontrarPorId(id);
            if (equipoExistente == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo no encontrado");
                return "redirect:/equipos";
            }

            // Validar campos obligatorios
            if (equipo.getDescEquipo() == null || equipo.getDescEquipo().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La descripción del equipo es obligatoria");
                return "redirect:/equipos/editar/" + id;
            }

            // Actualizar datos
            equipoExistente.setDescEquipo(equipo.getDescEquipo());

            equipoServicio.guardar(equipoExistente);
            redirectAttributes.addFlashAttribute("success", "Equipo actualizado exitosamente");
            return "redirect:/equipos?actualizacionExitosa=true";

        } catch (Exception e) {
            System.err.println("Error al actualizar equipo: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al actualizar equipo: " + e.getMessage());
            return "redirect:/equipos/editar/" + id;
        }
    }

    /**
     * Eliminar equipo
     */
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_EQUIPO')")
    public String eliminarEquipo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Equipo equipo = equipoServicio.encontrarPorId(id);
            if (equipo == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo no encontrado");
                return "redirect:/equipos";
            }

            // Verificar si tiene usuarios asociados
            if (equipo.getUsuarios() != null && !equipo.getUsuarios().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el equipo porque tiene usuarios asociados");
                return "redirect:/equipos";
            }

            // Verificar si tiene proyectos asociados
            if (equipo.getProyectos() != null && !equipo.getProyectos().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el equipo porque tiene proyectos asociados");
                return "redirect:/equipos";
            }

            equipoServicio.borrar(equipo);
            redirectAttributes.addFlashAttribute("success", "Equipo eliminado exitosamente");

        } catch (Exception e) {
            System.err.println("Error al eliminar equipo: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("constraint") || e.getMessage().contains("foreign key")) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el equipo porque tiene registros asociados");
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al eliminar equipo: " + e.getMessage());
            }
        }

        return "redirect:/equipos";
    }

    /**
     * Mostrar usuarios disponibles para asignar a equipo
     */
    @GetMapping("/{id}/usuarios")
    @PreAuthorize("hasAuthority('EDITAR_EQUIPO')")
    public String mostrarUsuariosParaAsignar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Equipo equipo = equipoServicio.encontrarPorId(id);
            if (equipo == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo no encontrado");
                return "redirect:/equipos";
            }

            List<Usuario> usuariosDisponibles = usuarioServicio.listarUsuariosSinEquipo();
            List<Usuario> usuariosEnEquipo = equipo.getUsuarios();

            model.addAttribute("equipo", equipo);
            model.addAttribute("usuariosDisponibles", usuariosDisponibles);
            model.addAttribute("usuariosEnEquipo", usuariosEnEquipo);

            return "equipos/asignar_usuarios";

        } catch (Exception e) {
            System.err.println("Error al cargar usuarios para asignar: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar usuarios");
            return "redirect:/equipos";
        }
    }

    /**
     * Asignar usuario a equipo
     */
    @PostMapping("/{idEquipo}/asignar-usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('EDITAR_EQUIPO')")
    public String asignarUsuarioAEquipo(
            @PathVariable Long idEquipo,
            @PathVariable Long idUsuario,
            RedirectAttributes redirectAttributes) {

        try {
            Equipo equipo = equipoServicio.encontrarPorId(idEquipo);
            Usuario usuario = usuarioServicio.encontrarPorId(idUsuario);

            if (equipo == null || usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Equipo o usuario no encontrado");
                return "redirect:/equipos";
            }

            // Asignar usuario al equipo
            usuario.setEquipo(equipo);
            usuarioServicio.guardar(usuario);

            redirectAttributes.addFlashAttribute("success", "Usuario asignado al equipo exitosamente");
            return "redirect:/equipos/detalle/" + idEquipo;

        } catch (Exception e) {
            System.err.println("Error al asignar usuario: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al asignar usuario");
            return "redirect:/equipos";
        }
    }

    /**
     * Remover usuario de equipo
     */
    @PostMapping("/{idEquipo}/remover-usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('EDITAR_EQUIPO')")
    public String removerUsuarioDeEquipo(
            @PathVariable Long idEquipo,
            @PathVariable Long idUsuario,
            RedirectAttributes redirectAttributes) {

        try {
            Usuario usuario = usuarioServicio.encontrarPorId(idUsuario);

            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/equipos";
            }

            // Remover usuario del equipo
            usuario.setEquipo(null);
            usuarioServicio.guardar(usuario);

            redirectAttributes.addFlashAttribute("success", "Usuario removido del equipo exitosamente");
            return "redirect:/equipos/detalle/" + idEquipo;

        } catch (Exception e) {
            System.err.println("Error al remover usuario: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al remover usuario");
            return "redirect:/equipos";
        }
    }
}