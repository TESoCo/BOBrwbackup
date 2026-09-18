package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/preciarios")
public class ControladorPreciario {

    @Autowired
    private PreciarioServicio preciarioServicio;

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping
    public String listarPreciarios(@RequestParam(required = false) String busqueda,
                                   Model model) {
        List<Preciario> preciarios;
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            preciarios = preciarioServicio.buscarPorNombre(busqueda);
        } else {
            preciarios = preciarioServicio.listarTodos();
        }
        model.addAttribute("preciarios", preciarios);
        model.addAttribute("searchTerm", busqueda);
        return "preciarios/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("preciario", new Preciario());
        model.addAttribute("apus", apuServicio.listarElementos());
        model.addAttribute("modo", "crear");
        return "preciarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Preciario preciario = preciarioServicio.obtenerPorId(id);
        if (preciario == null) {
            return "redirect:/preciarios?error=Preciario+no+encontrado";
        }
        model.addAttribute("preciario", preciario);
        model.addAttribute("apus", apuServicio.listarElementos());
        model.addAttribute("apusSeleccionados", preciario.getApus());
        model.addAttribute("modo", "editar");
        return "preciarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarPreciario(@ModelAttribute Preciario preciario,
                                   @RequestParam(required = false) List<Long> apuIds,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
            preciario.setIdUsuario(usuario);
            preciarioServicio.guardar(preciario);

            // Procesar APUs seleccionados
            if (apuIds != null && !apuIds.isEmpty()) {
                int orden = 0;
                for (Long apuId : apuIds) {
                    preciarioServicio.agregarApu(preciario.getIdPreciario(), apuId, orden++);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Preciario guardado correctamente");
            return "redirect:/preciarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar preciario: " + e.getMessage());
            return "redirect:/preciarios/nuevo";
        }
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarPreciario(@PathVariable Long id,
                                      @ModelAttribute Preciario preciario,
                                      @RequestParam(required = false) List<Long> apuIds,
                                      RedirectAttributes redirectAttributes) {
        try {
            Preciario existente = preciarioServicio.obtenerPorId(id);
            if (existente == null) {
                redirectAttributes.addFlashAttribute("error", "Preciario no encontrado");
                return "redirect:/preciarios";
            }

            existente.setNombrePreciario(preciario.getNombrePreciario());
            existente.setDescripcionPreciario(preciario.getDescripcionPreciario());
            preciarioServicio.guardar(existente);

            // Eliminar APUs actuales y agregar los nuevos
            List<Apu> apusActuales = preciarioServicio.obtenerApusDePreciario(id);
            for (Apu apu : apusActuales) {
                preciarioServicio.eliminarApu(id, apu.getIdAPU());
            }

            if (apuIds != null && !apuIds.isEmpty()) {
                int orden = 0;
                for (Long apuId : apuIds) {
                    preciarioServicio.agregarApu(id, apuId, orden++);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Preciario actualizado correctamente");
            return "redirect:/preciarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar preciario: " + e.getMessage());
            return "redirect:/preciarios/editar/" + id;
        }
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Preciario preciario = preciarioServicio.obtenerPorId(id);
        if (preciario == null) {
            return "redirect:/preciarios?error=Preciario+no+encontrado";
        }
        model.addAttribute("preciario", preciario);
        model.addAttribute("apus", preciarioServicio.obtenerApusDePreciario(id));
        return "preciarios/detalle";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarPreciario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Preciario preciario = preciarioServicio.obtenerPorId(id);
            if (preciario == null) {
                redirectAttributes.addFlashAttribute("error", "Preciario no encontrado");
                return "redirect:/preciarios";
            }
            preciarioServicio.eliminar(preciario);
            redirectAttributes.addFlashAttribute("success", "Preciario eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar preciario: " + e.getMessage());
        }
        return "redirect:/preciarios";
    }
}