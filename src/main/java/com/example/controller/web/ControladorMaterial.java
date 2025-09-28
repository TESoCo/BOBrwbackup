package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.Usuario;
import com.example.servicio.APUServicio;
import com.example.servicio.MaterialServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/material")
public class ControladorMaterial {

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private MaterialServicio materialServicio;

    @Autowired
    private DeepSeekService deepSeekService;



    @GetMapping("/inicioMaterial")
    public String inicioMaterial(Model model) {
        model.addAttribute("materiales", materialServicio.listarTodos());
        return "material/inicioMaterial";
    }

    @GetMapping("/crearMaterial")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("material", new Material());
        return "material/crearMaterial";
    }

    @PostMapping("/salvar")
    public String salvarMaterial(@ModelAttribute Material material, BindingResult result) {
        if (result.hasErrors()) {
            return "material/crearMaterial";
        }

        materialServicio.guardar(material);
        return "redirect:/material/inicioMaterial";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleMaterial(@PathVariable Long id, Model model) {
        Material material = materialServicio.obtenerPorId(id);
        model.addAttribute("material", material);
        return "material/detalleMaterial";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarMaterial(@PathVariable Long id) {
        materialServicio.eliminar(materialServicio.obtenerPorId(id).getIdMaterial());
        return "redirect:/material/inicioMaterial";
    }

    @PostMapping("/material/generarDesdeDescripcion")
    public String generarMaterialesDesdeDescripcion(
            @RequestParam String descripcionApu,
            @RequestParam(required = false) String nombreBase,
            Model model) {

        try {
            List<Map<String, String>> materialesGenerados =
                    deepSeekService.generarMaterialesDesdeDescripcion(descripcionApu);

            model.addAttribute("materialesGenerados", materialesGenerados);
            model.addAttribute("descripcionApu", descripcionApu);
            model.addAttribute("nombreBase", nombreBase != null ? nombreBase : "Materiales generados");
            model.addAttribute("material", new Material()); // Para el formulario principal

        } catch (Exception e) {
            model.addAttribute("error", "Error al generar materiales: " + e.getMessage());
        }

        return "material/crearMaterial";
    }

    @PostMapping("/material/guardarGenerados")
    public String guardarMaterialesGenerados(
            @RequestParam List<String> nombres,
            @RequestParam List<String> descripciones,
            @RequestParam List<String> unidades,
            @RequestParam List<String> precios,
            RedirectAttributes redirectAttributes) {

        try {
            int materialesGuardados = 0;

            for (int i = 0; i < nombres.size(); i++) {
                if (nombres.get(i) != null && !nombres.get(i).trim().isEmpty()) {
                    Material material = new Material();
                    material.setNombreMaterial(nombres.get(i).trim());
                    material.setDescripcionMaterial(descripciones.get(i).trim());
                    material.setUnidadMaterial(unidades.get(i).trim());
                    material.setPrecioMaterial(new BigDecimal(precios.get(i).trim()));

                    materialServicio.guardar(material);
                    materialesGuardados++;
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    materialesGuardados + " materiales guardados exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al guardar materiales: " + e.getMessage());
        }

        return "redirect:/material/inicioMaterial";
    }



}