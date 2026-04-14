package com.example.controller.web;

import com.example.dto.MaterialScrapedDTO;
import com.example.servicio.MaterialServicio;
import com.example.servicio.WebScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/scraper")
public class ControladorScraper {

    @Autowired
    private WebScraperService webScraperService;

    @Autowired
    private MaterialServicio materialServicio;

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("terminoBusqueda", "");
        return "scraper/panel";
    }

    @PostMapping("/buscar")
    public String buscarMateriales(@RequestParam String termino, Model model) {
        try {
            List<MaterialScrapedDTO> resultados = webScraperService.buscarMaterialesSincrono(termino);

            model.addAttribute("terminoBusqueda", termino);
            model.addAttribute("resultados", resultados);
            model.addAttribute("cantidad", resultados.size());

        } catch (Exception e) {
            model.addAttribute("error", "Error en la búsqueda: " + e.getMessage());
        }

        return "scraper/panel";
    }

    @PostMapping("/importar-todos")
    public String importarTodos(
            @RequestParam List<String> nombres,
            @RequestParam List<String> descripciones,
            @RequestParam List<String> unidades,
            @RequestParam List<String> precios,
            @RequestParam(required = false) List<String> proveedores,
            RedirectAttributes redirectAttributes) {

        List<MaterialScrapedDTO> materiales = new ArrayList<>();

        for (int i = 0; i < nombres.size(); i++) {
            MaterialScrapedDTO dto = new MaterialScrapedDTO();
            dto.setNombre(nombres.get(i));
            dto.setDescripcion(descripciones.get(i));
            dto.setUnidad(unidades.get(i));
            dto.setPrecio(new java.math.BigDecimal(precios.get(i)));
            if (proveedores != null && i < proveedores.size()) {
                dto.setProveedorNombre(proveedores.get(i));
            }
            materiales.add(dto);
        }

        int importados = webScraperService.importarMateriales(materiales);

        redirectAttributes.addFlashAttribute("success",
                "Se importaron " + importados + " materiales correctamente");

        return "redirect:/scraper/panel";
    }
}
