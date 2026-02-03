package com.example.controller.web;

import com.example.domain.Proyecto;
import com.example.domain.Equipo;
import com.example.domain.Obra;
import com.example.servicio.ProyectoServicio;
import com.example.servicio.EquipoServicio;
import com.example.servicio.ObraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/proyectos")
public class ControladorProyectos {

    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private EquipoServicio equipoServicio;

    @Autowired
    private ObraServicio obraServicio;

    /**
     * Mostrar la lista de proyectos
     */
    @GetMapping
    @PreAuthorize("hasAuthority('GESTIONAR_PROYECTOS') or hasAuthority('LEER_PROYECTO')")
    public String listarProyectos(Model model) {
        try {
            List<Proyecto> proyectos = proyectoServicio.listarProyectos();
            model.addAttribute("proyectos", proyectos);
            model.addAttribute("totalProyectos", proyectos != null ? proyectos.size() : 0);

            // Estadísticas
            if (proyectos != null) {
                long proyectosConEquipo = proyectos.stream()
                        .filter(p -> p.getEquipo() != null)
                        .count();
                long proyectosSinEquipo = proyectos.stream()
                        .filter(p -> p.getEquipo() == null)
                        .count();
                long proyectosConObras = proyectos.stream()
                        .filter(p -> p.getObras() != null && !p.getObras().isEmpty())
                        .count();

                model.addAttribute("proyectosConEquipo", proyectosConEquipo);
                model.addAttribute("proyectosSinEquipo", proyectosSinEquipo);
                model.addAttribute("proyectosConObras", proyectosConObras);
            }

        } catch (Exception e) {
            System.err.println("Error al listar proyectos: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("proyectos", List.of());
            model.addAttribute("totalProyectos", 0);
        }

        return "proyectos/proyectos_lista";
    }

    /**
     * Mostrar formulario para crear nuevo proyecto
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('CREAR_PROYECTO')")
    public String mostrarFormularioNuevo(Model model) {
        try {
            List<Equipo> equipos = equipoServicio.listarEquipos();
            model.addAttribute("equipos", equipos);
            model.addAttribute("modo", "crear");
            model.addAttribute("proyecto", new Proyecto());
        } catch (Exception e) {
            System.err.println("Error al cargar formulario nuevo proyecto: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("equipos", List.of());
        }

        return "proyectos/proyecto_formulario";
    }

    /**
     * Guardar nuevo proyecto
     */
    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('CREAR_PROYECTO')")
    public String guardarProyecto(
            @ModelAttribute Proyecto proyecto,
            @RequestParam(value = "equipo.idEquipo", required = false) Long idEquipo,
            RedirectAttributes redirectAttributes) {

        try {
            // Validar campos obligatorios
            if (proyecto.getDescProyecto() == null || proyecto.getDescProyecto().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La descripción del proyecto es obligatoria");
                return "redirect:/proyectos/nuevo";
            }

            // Asignar equipo si se seleccionó
            if (idEquipo != null) {
                Equipo equipo = equipoServicio.encontrarPorId(idEquipo);
                if (equipo != null) {
                    proyecto.setEquipo(equipo);
                }
            }

            proyectoServicio.guardar(proyecto);
            redirectAttributes.addFlashAttribute("success", "Proyecto creado exitosamente");
            return "redirect:/proyectos?creacionExitosa=true";

        } catch (Exception e) {
            System.err.println("Error al guardar proyecto: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al crear proyecto: " + e.getMessage());
            return "redirect:/proyectos/nuevo";
        }
    }

    /**
     * Mostrar detalles de un proyecto
     */
    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAuthority('LEER_PROYECTO')")
    public String mostrarDetalleProyecto(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Proyecto proyecto = proyectoServicio.encontrarPorId(id);
            if (proyecto == null) {
                redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado");
                return "redirect:/proyectos";
            }

            model.addAttribute("proyecto", proyecto);
            return "proyectos/proyecto_detalle";

        } catch (Exception e) {
            System.err.println("Error al cargar detalle del proyecto: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar proyecto");
            return "redirect:/proyectos";
        }
    }

    /**
     * Mostrar formulario para editar proyecto
     */
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_PROYECTO')")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Proyecto proyecto = proyectoServicio.encontrarPorId(id);
            if (proyecto == null) {
                redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado");
                return "redirect:/proyectos";
            }

            List<Equipo> equipos = equipoServicio.listarEquipos();
            model.addAttribute("proyecto", proyecto);
            model.addAttribute("equipos", equipos);
            model.addAttribute("modo", "editar");

            return "proyectos/proyecto_formulario";

        } catch (Exception e) {
            System.err.println("Error al cargar formulario de edición: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al cargar formulario de edición");
            return "redirect:/proyectos";
        }
    }

    /**
     * Actualizar proyecto existente
     */
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_PROYECTO')")
    public String actualizarProyecto(
            @PathVariable Long id,
            @ModelAttribute Proyecto proyecto,
            @RequestParam(value = "equipo.idEquipo", required = false) Long idEquipo,
            RedirectAttributes redirectAttributes) {

        try {
            // Buscar proyecto existente
            Proyecto proyectoExistente = proyectoServicio.encontrarPorId(id);
            if (proyectoExistente == null) {
                redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado");
                return "redirect:/proyectos";
            }

            // Validar campos obligatorios
            if (proyecto.getDescProyecto() == null || proyecto.getDescProyecto().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La descripción del proyecto es obligatoria");
                return "redirect:/proyectos/editar/" + id;
            }

            // Actualizar datos
            proyectoExistente.setDescProyecto(proyecto.getDescProyecto());

            // Actualizar equipo
            if (idEquipo != null) {
                Equipo equipo = equipoServicio.encontrarPorId(idEquipo);
                proyectoExistente.setEquipo(equipo);
            } else {
                proyectoExistente.setEquipo(null); // Remover equipo si no se seleccionó
            }

            proyectoServicio.guardar(proyectoExistente);
            redirectAttributes.addFlashAttribute("success", "Proyecto actualizado exitosamente");
            return "redirect:/proyectos?actualizacionExitosa=true";

        } catch (Exception e) {
            System.err.println("Error al actualizar proyecto: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al actualizar proyecto: " + e.getMessage());
            return "redirect:/proyectos/editar/" + id;
        }
    }

    /**
     * Eliminar proyecto
     */
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_PROYECTO')")
    public String eliminarProyecto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Proyecto proyecto = proyectoServicio.encontrarPorId(id);
            if (proyecto == null) {
                redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado");
                return "redirect:/proyectos";
            }

            // Verificar si tiene obras asociadas
            if (proyecto.getObras() != null && !proyecto.getObras().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el proyecto porque tiene obras asociadas");
                return "redirect:/proyectos";
            }

            proyectoServicio.borrar(proyecto);
            redirectAttributes.addFlashAttribute("success", "Proyecto eliminado exitosamente");

        } catch (Exception e) {
            System.err.println("Error al eliminar proyecto: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("constraint") || e.getMessage().contains("foreign key")) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el proyecto porque tiene registros asociados");
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al eliminar proyecto: " + e.getMessage());
            }
        }

        return "redirect:/proyectos";
    }
}