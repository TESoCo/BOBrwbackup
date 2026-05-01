package com.example.controller.web;

import com.example.dto.MaterialScrapedDTO;
import com.example.servicio.MaterialServicio;
import com.example.servicio.WebScraperService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/buscar-con-redireccion")
    public String buscarConRedireccion(
            @RequestParam String termino,
            @RequestParam Long materialId,
            @RequestParam String nombreMaterial,
            @RequestParam(required = false) String returnUrl,
            Model model,
            HttpSession session) {

        // Guardar en sesión el contexto de la cotización
        session.setAttribute("cotizacionContexto", Map.of(
                "termino", termino,
                "materialId", materialId,
                "returnUrl", returnUrl != null ? returnUrl : "/material/crearMaterial"
        ));

        // Ejecutar búsqueda
        List<MaterialScrapedDTO> resultados = webScraperService.buscarMaterialesSincrono(termino);

        model.addAttribute("resultados", resultados);
        model.addAttribute("cantidad", resultados.size());
        model.addAttribute("terminoBusqueda", termino);
        model.addAttribute("modoCotizacion", true);
        model.addAttribute("materialId", materialId);
        model.addAttribute("returnUrl", returnUrl);

        return "scraper/panel";
    }

    @PostMapping("/importar-todos")
    public String importarTodos(
            @RequestParam List<String> nombres,
            @RequestParam List<String> descripciones,
            @RequestParam List<String> unidades,
            @RequestParam List<String> precios,
            @RequestParam(required = false) List<String> proveedores,
            @RequestParam(required = false) List<String> nits,
            @RequestParam(required = false) List<String> correos,
            @RequestParam(required = false) List<String> telefonos,
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
            if (nits != null && i < nits.size()) {
                dto.setProveedorNit(nits.get(i));
            }
            if (correos != null && i < correos.size()) {
                dto.setProveedorCorreo(correos.get(i));
            }
            if (telefonos != null && i < telefonos.size()) {
                dto.setProveedorTelefono(telefonos.get(i));
            }

            materiales.add(dto);
        }

        int importados = webScraperService.importarMateriales(materiales);

        redirectAttributes.addFlashAttribute("success",
                "Se importaron " + importados + " materiales correctamente");

        return "redirect:/scraper/panel";
    }

    // Endpoint para aplicar cotización a un material existente
    @PostMapping("/aplicar-cotizacion")
    public String aplicarCotizacion(
            @RequestParam Long materialId,
            @RequestParam String nombreMaterial,
            @RequestParam String proveedorNombre,
            @RequestParam BigDecimal precio,
            @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String descripcion,
            RedirectAttributes redirectAttributes) {

        try {
            // Crear DTO con los datos del scraper
            MaterialScrapedDTO dto = new MaterialScrapedDTO();
            dto.setNombre(nombreMaterial);
            dto.setProveedorNombre(proveedorNombre);
            dto.setPrecio(precio);
            dto.setUnidad(unidad != null ? unidad : "und");
            dto.setDescripcion(descripcion != null ? descripcion : nombreMaterial);
            dto.setFechaScraping(LocalDateTime.now());

            // Valores por defecto para campos requeridos
            if (dto.getProveedorNit() == null) dto.setProveedorNit("NIT_COTIZACION_" + System.currentTimeMillis());
            if (dto.getProveedorCorreo() == null) dto.setProveedorCorreo("cotizacion@proveedor.com");
            if (dto.getProveedorTelefono() == null) dto.setProveedorTelefono("0000000");

            // Actualizar el precio del material
            boolean actualizado = webScraperService.actualizarPrecioMaterialDesdeScraper(materialId, dto);

            if (actualizado) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("✅ Precio actualizado para '%s' con proveedor %s: $%,.2f COP",
                                nombreMaterial, proveedorNombre, precio));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ No se pudo actualizar el precio del material");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error al actualizar precio: " + e.getMessage());
        }

        return "redirect:/material/inicioMaterial";
    }

}
