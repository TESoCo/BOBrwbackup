package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.PrecioMaterial;
import com.example.domain.Usuario;
import com.example.servicio.APUServicio;
import com.example.servicio.APUServicioImp;
import com.example.servicio.MaterialServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicioWeb.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.*;

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

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EnhancedAgentAIService enhancedAgentAIService;

    @GetMapping("/inicioMaterial")
    public String inicioMaterial(Model model, Authentication authentication) {
        model.addAttribute("materiales", materialServicio.listarTodos());
        model.addAttribute("apus",apuServicio.listarElementos());

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

    @GetMapping("/buscarApu")
    @ResponseBody
    public List<Apu> buscarApu(@RequestParam String termino) {
        return apuServicio.buscarPorNombre(termino);
    }



    //ayudante para interacciones con el agente
    @GetMapping("/obtenerMaterialesActuales")
    @ResponseBody
    public List<Map<String, String>> obtenerMaterialesActuales(HttpSession session) {
        String sessionId = session.getId();
        AgentConversation conversation = conversationService.getConversation(sessionId);
        if (conversation != null && conversation.getUltimosMateriales() != null) {
            return conversation.getUltimosMateriales();
        }
        return new ArrayList<>();
    }

    // Endpoint para obtener precios de un material
    @GetMapping("/{materialId}/precios")
    @ResponseBody
    public List<PrecioMaterial> obtenerPreciosMaterial(@PathVariable Long materialId) {
        return materialServicio.getPreciosPorMaterial(materialId);
    }
    // Endpoint para asignar precio a material con proveedor
    @PostMapping("/{materialId}/precio")
    @ResponseBody
    public Map<String, Object> asignarPrecio(
            @PathVariable Long materialId,
            @RequestParam Long proveedorId,
            @RequestParam BigDecimal precio,
            @RequestParam(required = false) BigDecimal precioMinimoCompra) {

        Map<String, Object> response = new HashMap<>();
        try {
            PrecioMaterial precioMaterial = materialServicio.asignarPrecioAProveedor(
                    materialId, proveedorId, precio, precioMinimoCompra);
            response.put("success", true);
            response.put("precio", precioMaterial);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    // Endpoint para calcular APU con proveedor específico
    @GetMapping("/apu/{apuId}/calcularConProveedor")
    @ResponseBody
    public Map<String, Object> calcularAPUConProveedor(
            @PathVariable Long apuId,
            @RequestParam Long proveedorId,
            @Autowired APUServicioImp apuServicioImp) {

        Map<String, Object> response = new HashMap<>();
        try {
            BigDecimal costoMateriales = apuServicioImp.calcularCostoMaterialesConProveedor(apuId, proveedorId);
            Apu apu = apuServicio.obtenerPorId(apuId);

            response.put("success", true);
            response.put("apuId", apuId);
            response.put("proveedorId", proveedorId);
            response.put("costoMateriales", costoMateriales);
            response.put("costoManoObra", apu.getVManoDeObraAPU());
            response.put("costoTransporte", apu.getVTransporteAPU());
            response.put("costoMisc", apu.getVMiscAPU());
            response.put("costoTotal", costoMateriales
                    .add(apu.getVManoDeObraAPU() != null ? apu.getVManoDeObraAPU() : BigDecimal.ZERO)
                    .add(apu.getVTransporteAPU() != null ? apu.getVTransporteAPU() : BigDecimal.ZERO)
                    .add(apu.getVMiscAPU() != null ? apu.getVMiscAPU() : BigDecimal.ZERO));
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //ENDPOINTS PARA IA
    ////////////////////////////////////////////////////////////////////////////////////////////

    //GENERADOR FLEX
    //Generar materiales desde APU
    @PostMapping("/generarDesdeApu")
    public String generarMaterialesDesdeApu(
            @RequestParam Long apuId,
            @RequestParam(defaultValue = "import") String tabActiva, //pestaña activa
            Model model) {

        System.out.println("=== GENERAR MATERIALES DESDE APU ===");
        System.out.println("APU ID recibido: " + apuId);
        System.out.println("Pestaña activa: " + tabActiva);

        try {
            // Obtener el APU seleccionado
            Apu apuSeleccionado = apuServicio.obtenerPorId(apuId);

            if (apuSeleccionado == null) {
                model.addAttribute("error", "APU no encontrado");
                return recargarFormulario(model, tabActiva); // Pasar la pestaña
            }

            System.out.println("APU encontrado: " + apuSeleccionado.getNombreAPU());

            // USAR SERVICIO HÍBRIDO
            List<Map<String, String>> materialesGenerados =
                    aiService.generarMateriales(apuSeleccionado.getDescAPU());

            System.out.println("Materiales generados: " + materialesGenerados.size());

            // VALIDAR ESTRUCTURA DE CADA MATERIAL
            for (Map<String, String> material : materialesGenerados) {
                if (!material.containsKey("nombre")) {
                    material.put("nombre", "ERROR Material sin nombre");
                }
                if (!material.containsKey("descripcion")) {
                    material.put("descripcion", "Descripción no disponible");
                }
                if (!material.containsKey("unidad")) {
                    material.put("unidad", "und");
                }
                if (!material.containsKey("precio")) {
                    material.put("precio", "1000.00");
                }
            }

            model.addAttribute("materialesGenerados", materialesGenerados);
            model.addAttribute("apuSeleccionado", apuSeleccionado);
            model.addAttribute("success", "Se generaron " + materialesGenerados.size() + " materiales usando IA");
            model.addAttribute("tabActiva", tabActiva); // Enviar pestaña a la vista

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            model.addAttribute("error", "Error al generar materiales: " + e.getMessage());
        }

        return recargarFormulario(model, tabActiva); // Pasar la pestaña
    }

    //Generar materiales desde descripcion
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

    //Guardar materiales del generador
    @PostMapping("/guardarGenerados")
    public String guardarMaterialesGenerados(
            @RequestParam List<String> nombres,
            @RequestParam List<String> descripciones,
            @RequestParam List<String> unidades,
            @RequestParam List<String> precios,
            @RequestParam(required = false) List<String> seleccionados,// Hacerlo opcional
            @RequestParam(required = false) Long proveedorId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        System.out.println("=== INTENTANDO GUARDAR MATERIALES ===");
        System.out.println("Nombres recibidos: " + nombres.size());
        System.out.println("Descripciones: " + descripciones.size());
        System.out.println("Unidades: " + unidades.size());
        System.out.println("Precios: " + precios.size());
        System.out.println("Seleccionados: " + (seleccionados != null ? seleccionados.size() : "null"));

        try {
            int materialesGuardados = 0;

            // Obtener usuario actual para asociar proveedores si es necesario
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

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

                        // TODO: corregir la forma de Manejar el precio, adaptar a los precios relacionados con proveedor

                        // Guardar el material primero
                        materialServicio.guardar(material);

                        // Si hay un proveedor asociado, asignar el precio
                        if (proveedorId != null && precios != null && i < precios.size()) {
                            try {
                                BigDecimal precio = new BigDecimal(precios.get(i).trim());
                                materialServicio.asignarPrecioAProveedor(
                                        material.getIdMaterial(),
                                        proveedorId,
                                        precio,
                                        null
                                );
                                System.out.println(" Precio asignado para proveedor: " + precio);
                            } catch (NumberFormatException e) {
                                System.out.println("⚠️ Precio inválido para " + nombre);
                            }
                        }

                        materialesGuardados++;
                    }
                }
            }

            System.out.println("Materiales guardados exitosamente: " + materialesGuardados);
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

    //Endpoint para pruebas GENERADOR FLEX
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

        // Probar Gemini individualmente
        try {
            List<Map<String, String>> geminiResult = geminiService.generarMaterialesDesdeDescripcion("Prueba conexión");
            resultado.append("✅ Gemini: FUNCIONA (").append(geminiResult.size()).append(" materiales)\n");
        } catch (Exception e) {
            resultado.append("❌ Gemini: FALLÓ - ").append(e.getMessage()).append("\n");
        }


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


    //AGENTE IA (chat)
    //TODO: Generar desde APU usando el generador Flex, luego pasa de vuelta el feedback al mismo generador...
    //  pero ¿no queda un prompt dentro del otro?
    @PostMapping("/generarConAgente")
    @ResponseBody
    public Map<String, Object> generarConAgente(
            @RequestParam Long apuId,
            @RequestParam(required = false) String feedback,
            HttpSession session,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Apu apu = apuServicio.obtenerPorId(apuId);
            if (apu == null) {
                response.put("error", "APU no encontrado");
                return response;
            }

            String sessionId = session.getId();
            String userId = authentication.getName();

            AgentResponse agentResponse = enhancedAgentAIService.generarMaterialesConContexto(
                    sessionId, userId, apu, feedback
            );

            response.put("materiales", agentResponse.getMateriales());
            response.put("mensaje", agentResponse.getMensaje());
            response.put("sugerencias", agentResponse.getSugerencias());
            response.put("preguntas", agentResponse.getPreguntas());
            response.put("success", true);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("success", false);
        }

        return response;
    }

    //Estado de cache de conversacion
    @GetMapping("/conversacion/estado")
    @ResponseBody
    public Map<String, Object> getConversacionEstado(HttpSession session) {
        String sessionId = session.getId();
        AgentConversation conv = conversationService.getConversation(sessionId);

        Map<String, Object> response = new HashMap<>();
        if (conv != null) {
            response.put("interacciones", conv.getInteractionCount());
            response.put("ultimaActualizacion", conv.getLastUpdated());
            response.put("materialesGenerados", conv.getUltimosMateriales() != null ?
                    conv.getUltimosMateriales().size() : 0);
        } else {
            response.put("activa", false);
        }
        return response;
    }

    //Endpoint para pruebas memoria cache de la conversacion (redis)
    @GetMapping("/test-redis-simple")
    @ResponseBody
    public String testRedisSimple() {
        try {
            // Probar con un objeto simple primero
            redisTemplate.opsForValue().set("test:simple", "Hola Redis!");
            String result = (String) redisTemplate.opsForValue().get("test:simple");

            if ("Hola Redis!".equals(result)) {
                return "✅ Redis básico funciona!";
            } else {
                return "❌ Redis básico falló";
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    @GetMapping("/test-redis-conversation")
    @ResponseBody
    public String testRedisConversation() {
        try {
            // Crear una conversación simple sin APU
            AgentConversation test = new AgentConversation();
            test.setUserId("test-user");
            test.setApuId(1L);
            test.setApuNombre("APU de prueba");
            test.setInteractionCount(1);

            conversationService.saveConversation("test-session-2", test);
            AgentConversation retrieved = conversationService.getConversation("test-session-2");

            if (retrieved != null && "test-user".equals(retrieved.getUserId())) {
                return "✅ Conversación guardada y recuperada correctamente!";
            } else {
                return "❌ Falló la recuperación de conversación";
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    // Limpiar cache de conversacion
    @GetMapping("/limpiarCache")
    @ResponseBody
    public String limpiarCache() {
        // Esto es para limpiar
        // En desarrollo, puedes limpiar desde Redis CLI: redis-cli FLUSHALL
        return "Para limpiar caché, ejecuta: redis-cli FLUSHALL";
    }

    //Endpoint para aplicar feedback de usuario a los materiales generados
    @PostMapping("/refinarMateriales")
    @ResponseBody
    public Map<String, Object> refinarMateriales(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long apuId = Long.valueOf(request.get("apuId").toString());
            String feedback = (String) request.get("feedback");
            List<Map<String, String>> materialesActuales = (List<Map<String, String>>) request.get("materialesActuales");

            String sessionId = (String) request.get("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                // Fallback: obtener de la cookie de sesión
                jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    for (jakarta.servlet.http.Cookie cookie : cookies) {
                        if ("SESSION".equals(cookie.getName())) {
                            sessionId = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            if (sessionId == null) {
                sessionId = httpRequest.getSession().getId();
            }

            String userId = authentication.getName();

            Apu apu = apuServicio.obtenerPorId(apuId);

            // Construir prompt con los materiales actuales y el feedback
            StringBuilder prompt = new StringBuilder();
            prompt.append("Lista actual de materiales:\n");
            for (Map<String, String> m : materialesActuales) {
                prompt.append("- ").append(m.get("nombre"))
                        .append(" (").append(m.get("unidad")).append("): ")
                        .append(m.get("descripcion")).append("\n");
            }
            prompt.append("\nFeedback del usuario: ").append(feedback);
            prompt.append("\n\nPor favor, ajusta la lista de materiales según el feedback. ");
            prompt.append("Responde SOLO con un array JSON como antes.");

            // Usar IA para refinar
            List<Map<String, String>> materialesRefinados = aiService.generarMateriales(prompt.toString());

            // Guardar en la conversación
            AgentConversation conversation = conversationService.getConversation(sessionId);
            if (conversation == null) {
                conversation = new AgentConversation(userId, apu);
            }
            conversation.setUltimosMateriales(materialesRefinados);
            conversation.getHistorialFeedback().add(feedback);
            conversation.setInteractionCount(conversation.getInteractionCount() + 1);
            conversationService.saveConversation(sessionId, conversation);

            response.put("success", true);
            response.put("materiales", materialesRefinados);
            response.put("mensaje", "Materiales refinados según tu feedback");
            response.put("sugerencias", List.of("¿Necesitas ajustar algo más?", "¿Las cantidades son correctas?"));

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////




    //Endpoint auxiliar para frontend
    private String recargarFormulario(Model model, String tabActiva) {
        List<Apu> apus = apuServicio.listarElementos();
        model.addAttribute("apus", apus);
        model.addAttribute("material", new Material());
        model.addAttribute("tabActiva", tabActiva); //  Pasar pestaña a la vista
        return "material/crearMaterial";
    }














}