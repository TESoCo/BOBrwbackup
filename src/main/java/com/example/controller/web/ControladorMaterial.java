package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.Usuario;
import com.example.servicio.APUServicio;
import com.example.servicio.MaterialServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.AIService;
import com.example.servicioWeb.DeepSeekService;
import com.example.servicioWeb.OpenRouterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
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

    @Autowired
    private OpenRouterService openRouterService;

    @Autowired
    private AIService aiService;



    @GetMapping("/inicioMaterial")
    public String inicioMaterial(Model model, Authentication authentication) {
        model.addAttribute("materiales", materialServicio.listarTodos());
        model.addAttribute("apus",apuServicio.listarElementos());

        //INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            // Debug información del usuario
            System.out.println("Usuario autenticado: " + username);
            System.out.println("Autoridades: " + authorities);

            // Agregar información específica del usuario al modelo
            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            // Verificar roles específicos
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);
        }

        return "material/inicioMaterial";
    }

    @GetMapping("/crearMaterial")
    public String mostrarFormularioCrear(Model model) {
        // Obtener lista de APUs para el selector
        List<Apu> apus = apuServicio.listarElementos();
        model.addAttribute("apus", apus);
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

    // Mét0do GET para mostrar el formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        Material material = materialServicio.obtenerPorId(id);
        model.addAttribute("material", material);
        return "material/editarMaterial";
    }

    // Métod0 POST para procesar el formulario de edición
    @PostMapping("/editar/{id}")
    public String editarMaterial(@PathVariable Long id,
                                 @ModelAttribute Material material,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Asegurar que el ID se mantenga
            material.setIdMaterial(id);
            materialServicio.guardar(material);
            redirectAttributes.addFlashAttribute("successMessage", "Material actualizado correctamente");
            return "redirect:/material/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar el material: " + e.getMessage());
            return "redirect:/material/editar/" + id;
        }
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

    @PostMapping("/generarDesdeDescripcion")
    public String generarMaterialesDesdeDescripcion(
            @RequestParam String descripcionApu,
            @RequestParam(required = false) String nombreBase,
            Model model) {

        try {
            //  USAR SERVICIO HÍBRIDO EN LUGAR DE DEEPSEEK
            List<Map<String, String>> materialesGenerados =
                    aiService.generarMateriales(descripcionApu);

            model.addAttribute("materialesGenerados", materialesGenerados);
            model.addAttribute("descripcionApu", descripcionApu);
            model.addAttribute("nombreBase", nombreBase != null ? nombreBase : "Materiales generados");
            model.addAttribute("material", new Material()); // Para el formulario principal

        } catch (Exception e) {
            model.addAttribute("error", "Error al generar materiales: " + e.getMessage());
        }

        return "material/crearMaterial";
    }

    @PostMapping("/guardarGenerados")
    public String guardarMaterialesGenerados(
            @RequestParam List<String> nombres,
            @RequestParam List<String> descripciones,
            @RequestParam List<String> unidades,
            @RequestParam List<String> precios,
            @RequestParam(required = false) List<String> seleccionados, // Hacerlo opcional
            RedirectAttributes redirectAttributes) {

        System.out.println("=== INTENTANDO GUARDAR MATERIALES ===");
        System.out.println("Nombres recibidos: " + nombres.size());
        System.out.println("Descripciones: " + descripciones.size());
        System.out.println("Unidades: " + unidades.size());
        System.out.println("Precios: " + precios.size());
        System.out.println("Seleccionados: " + (seleccionados != null ? seleccionados.size() : "null"));

        try {
            int materialesGuardados = 0;

            for (int i = 0; i < nombres.size(); i++) {
                // Si no hay seleccionados, guardar todos
                boolean guardar = seleccionados == null ||
                        i >= seleccionados.size() ||
                        "true".equals(seleccionados.get(i));

                if (guardar) {
                    String nombre = nombres.get(i);
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        Material material = new Material();
                        material.setNombreMaterial(nombre.trim());
                        material.setDescripcionMaterial(descripciones.get(i).trim());
                        material.setUnidadMaterial(unidades.get(i).trim());

                        // Manejar el precio
                        String precioStr = precios.get(i).trim();
                        try {
                            BigDecimal precio = new BigDecimal(precioStr);
                            material.setPrecioMaterial(precio);
                            System.out.println("💾 Guardando: " + nombre + " - $" + precio);
                        } catch (NumberFormatException e) {
                            material.setPrecioMaterial(BigDecimal.ZERO);
                            System.out.println("⚠️  Precio inválido para " + nombre + ", usando $0");
                        }

                        materialServicio.guardar(material);
                        materialesGuardados++;
                    }
                }
            }

            System.out.println("✅ Materiales guardados exitosamente: " + materialesGuardados);
            redirectAttributes.addFlashAttribute("success",
                    materialesGuardados + " materiales guardados exitosamente");

        } catch (Exception e) {
            System.out.println("❌ Error guardando materiales: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al guardar materiales: " + e.getMessage());
        }


        return "redirect:/material/inicioMaterial";
    }

    @PostMapping("/generarDesdeApu")
    public String generarMaterialesDesdeApu(
            @RequestParam Long apuId,
            @RequestParam(defaultValue = "import") String tabActiva, //  Nuevo parámetro
            Model model) {

        System.out.println("=== GENERAR MATERIALES DESDE APU ===");
        System.out.println("APU ID recibido: " + apuId);
        System.out.println("Pestaña activa: " + tabActiva);

        try {
            // Obtener el APU seleccionado
            Apu apuSeleccionado = apuServicio.obtenerPorId(apuId);

            if (apuSeleccionado == null) {
                model.addAttribute("error", "APU no encontrado");
                return recargarFormulario(model, tabActiva); // ✅ Pasar la pestaña
            }

            System.out.println("APU encontrado: " + apuSeleccionado.getNombreAPU());

            // USAR SERVICIO HÍBRIDO
            List<Map<String, String>> materialesGenerados =
                    aiService.generarMateriales(apuSeleccionado.getDescAPU());

            System.out.println("Materiales generados: " + materialesGenerados.size());

            model.addAttribute("materialesGenerados", materialesGenerados);
            model.addAttribute("apuSeleccionado", apuSeleccionado);
            model.addAttribute("success", "Se generaron " + materialesGenerados.size() + " materiales usando IA");
            model.addAttribute("tabActiva", tabActiva); // ✅ Enviar pestaña a la vista

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            model.addAttribute("error", "Error al generar materiales: " + e.getMessage());
        }

        return recargarFormulario(model, tabActiva); // ✅ Pasar la pestaña
    }



    private String recargarFormulario(Model model, String tabActiva) {
        List<Apu> apus = apuServicio.listarElementos();
        model.addAttribute("apus", apus);
        model.addAttribute("material", new Material());
        model.addAttribute("tabActiva", tabActiva); //  Pasar pestaña a la vista
        return "material/crearMaterial";
    }

    @GetMapping("/probarServiciosIA")
    @ResponseBody
    public String probarServiciosIA() {
        StringBuilder resultado = new StringBuilder();
        resultado.append("=== PRUEBA SERVICIOS IA ===\n\n");

        String descripcionPrueba = "Muro de contención en gaviones con piedra y geotextil";

        resultado.append("Descripción: ").append(descripcionPrueba).append("\n\n");

        // Probar servicio híbrido
        try {
            List<Map<String, String>> materiales = aiService.generarMateriales(descripcionPrueba);
            resultado.append("✅ SERVICIO HÍBRIDO FUNCIONA!\n");
            resultado.append("Materiales generados: ").append(materiales.size()).append("\n");
            for (Map<String, String> material : materiales) {
                resultado.append("  - ").append(material.get("nombre"))
                        .append(" (").append(material.get("unidad")).append(") $")
                        .append(material.get("precio")).append("\n");
            }
        } catch (Exception e) {
            resultado.append("❌ SERVICIO HÍBRIDO FALLÓ: ").append(e.getMessage()).append("\n");
        }

        resultado.append("\n--- Pruebas individuales ---\n");

        // Probar DeepSeek individualmente
        try {
            List<Map<String, String>> materialesDeepSeek = deepSeekService.generarMaterialesDesdeDescripcion(descripcionPrueba);
            resultado.append("✅ DEEPSEEK FUNCIONA: ").append(materialesDeepSeek.size()).append(" materiales\n");
        } catch (Exception e) {
            resultado.append("❌ DEEPSEEK FALLÓ: ").append(e.getMessage()).append("\n");
        }

        // Probar OpenRouter individualmente
        try {
            List<Map<String, String>> materialesOpenRouter = openRouterService.generarMaterialesDesdeDescripcion(descripcionPrueba);
            resultado.append("✅ OPENROUTER FUNCIONA: ").append(materialesOpenRouter.size()).append(" materiales\n");
        } catch (Exception e) {
            resultado.append("❌ OPENROUTER FALLÓ: ").append(e.getMessage()).append("\n");
        }

        return resultado.toString();
    }


    @GetMapping("/buscarApu")
    @ResponseBody
    public List<Apu> buscarApu(@RequestParam String termino) {
        return apuServicio.buscarPorNombre(termino);
    }

}