// GeminiService.java
package com.example.servicioWeb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ============================================
    // MÉTODOS PÚBLICOS
    // ============================================

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== GEMINI SERVICE ===");
        System.out.println("Descripción: " + descripcionApu);

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("tu_api_key")) {
            System.out.println("❌ Gemini API Key no configurada");
            throw new RuntimeException("API Key de Gemini no configurada");
        }

        try {
            System.out.println("🔄 Llamando a Gemini API...");
            String prompt = crearPromptMateriales(descripcionApu);
            String respuesta = llamarGeminiAPI(prompt);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);
            System.out.println("Gemini exitoso: " + materiales.size() + " materiales");
            return materiales;

        } catch (Exception e) {
            System.out.println("❌ Error Gemini: " + e.getMessage());
            throw new RuntimeException("Error al generar materiales con Gemini: " + e.getMessage());
        }
    }

    /**
     * Estima cantidades
     */
    public List<Map<String, String>> estimarCantidades(String descripcionApu, List<Map<String, String>> materiales, String unidadBase) {
        System.out.println("=== GEMINI - ESTIMACIÓN DE CANTIDADES ===");

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("tu_api_key")) {
            throw new RuntimeException("API Key de Gemini no configurada");
        }

        try {
            String prompt = crearPromptCantidades(descripcionApu, materiales, unidadBase);
            String respuesta = llamarGeminiAPI(prompt);
            return parsearRespuestaCantidades(respuesta);
        } catch (Exception e) {
            System.out.println("❌ Error Gemini estimación: " + e.getMessage());
            throw new RuntimeException("Error al estimar cantidades: " + e.getMessage());
        }
    }

    // ============================================
    // MÉTODOS PRIVADOS DE PROMPT
    // ============================================

    private String crearPromptMateriales(String descripcionApu) {
        return "Eres un experto en construcción. Analiza esta descripción y genera una lista de materiales necesarios. " +
                "Responde SOLO con un array JSON válido. Cada objeto debe tener exactamente: nombre, descripcion, unidad, precio, proveedor.\n\n" +
                "Unidades permitidas: m³, m², m, kg, und, gl, l, hr, día, viaje, juego\n\n" +
                "Descripción: " + descripcionApu + "\n\n" +
                "Ejemplo de formato:\n" +
                "[{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para construcción\", \"unidad\": \"kg\", \"precio\": \"10000.00\", \"proveedor\": \"Alkosto\"}]\n\n" +
                "Genera entre 5-8 materiales relevantes para la actividad descrita.";
    }

    private String crearPromptCantidades(String descripcionApu, List<Map<String, String>> materiales, String unidadBase) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un ingeniero civil experto en construcción y presupuestos.\n\n");
        sb.append("ACTIVIDAD A REALIZAR (para 1 UNIDAD del APU):\n");
        sb.append(descripcionApu).append("\n\n");
        sb.append("UNIDAD DE MEDIDA BASE DEL APU: ").append(unidadBase).append("\n\n");
        sb.append("MATERIALES NECESARIOS (sin cantidades aún):\n");

        for (Map<String, String> material : materiales) {
            sb.append("- ").append(material.get("nombre"));
            sb.append(" (").append(material.get("unidad")).append("): ");
            sb.append(material.get("descripcion")).append("\n");
        }

        sb.append("""

            INSTRUCCIONES IMPORTANTES:
            1. Estima las CANTIDADES FÍSICAS requeridas de CADA material para realizar 1 UNIDAD de la actividad descrita.
            2. NO confundas cantidad con precio. La cantidad es cuánto material se necesita.
            3. Considera desperdicios (10-15%) y factores de seguridad.
            4. Responde SOLO con un array JSON. Cada objeto debe tener: nombre, cantidad, unidad, justificacion.
            5. La "cantidad" debe ser un NÚMERO (ej: 45.50, no "$45.50").
            6. La "unidad" debe ser la misma que la del material listado.

            EJEMPLO CORRECTO:
            [
              {"nombre": "Cemento", "cantidad": 45.50, "unidad": "kg", "justificacion": "Para 2 m³ de concreto"},
              {"nombre": "Arena", "cantidad": 3.80, "unidad": "m³", "justificacion": "Proporción 1:3"}
            ]

            AHORA GENERA TU RESPUESTA (SOLO EL ARRAY JSON):
            """);

        return sb.toString();
    }

    // ============================================
    // MÉTODOS DE API
    // ============================================

    private String llamarGeminiAPI(String prompt) throws Exception {

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> contents = new HashMap<>();

        contents.put("parts", List.of(Map.of("text", prompt)));

        requestBody.put("contents", List.of(contents));
        requestBody.put("generationConfig", Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 2000
        ));

        String urlCompleta = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        System.out.println("Enviando petición a Gemini...");

        ResponseEntity<String> response = restTemplate.exchange(
                urlCompleta, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();
        } else {
            throw new Exception("Error " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    // ============================================
    // MÉTODOS DE PARSING
    // ============================================

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            // Limpiar la respuesta
            //String json = respuesta.trim();
            //json = json.replace("```json", "").replace("```", "").trim();
            String json = limpiarRespuesta(respuesta);

            System.out.println(" Parseando respuesta Gemini: " + json.substring(0, Math.min(100, json.length())) + "...");

            JsonNode array = objectMapper.readTree(json);
            List<Map<String, String>> materiales = new ArrayList<>();

            for (JsonNode item : array) {
                Map<String, String> material = new HashMap<>();
                material.put("nombre", item.path("nombre").asText());
                material.put("descripcion", item.path("descripcion").asText());
                material.put("unidad", item.path("unidad").asText());
                material.put("precio", "1000.00");
                material.put("proveedor", "");
                materiales.add(material);
                System.out.println(" " + material.get("nombre") + " (" + material.get("unidad") + ")");
            }

            return materiales;
        } catch (Exception e) {
            System.out.println("❌ Error parseando respuesta Gemini: " + e.getMessage());
            System.out.println("Respuesta cruda: " + respuesta);
            throw new Exception("Gemini no respondió en formato JSON válido");
        }
    }

    private List<Map<String, String>> parsearRespuestaCantidades(String respuesta) throws Exception {
        try {
            String json = limpiarRespuesta(respuesta);
            System.out.println("🔧 JSON de cantidades Gemini: " + json);

            JsonNode array = objectMapper.readTree(json);
            List<Map<String, String>> resultados = new ArrayList<>();

            for (JsonNode item : array) {
                Map<String, String> resultado = new HashMap<>();

                // Obtener nombre
                String nombre = item.has("nombre") ? item.path("nombre").asText() : "";
                if (nombre == null || nombre.isEmpty()) {
                    // Intentar con "material" si existe
                    nombre = item.has("material") ? item.path("material").asText() : "";
                }

                // Obtener cantidad - probar diferentes campos
                String cantidad = "0";
                if (item.has("cantidad")) {
                    cantidad = item.path("cantidad").asText();
                } else if (item.has("cantidadEstimada")) {
                    cantidad = item.path("cantidadEstimada").asText();
                } else if (item.has("cantidad_estimada")) {
                    cantidad = item.path("cantidad_estimada").asText();
                } else if (item.has("quantity")) {
                    cantidad = item.path("quantity").asText();
                } else if (item.has("valor")) {
                    cantidad = item.path("valor").asText();
                }

                // Si la cantidad es un número, asegurar formato
                try {
                    double cant = Double.parseDouble(cantidad);
                    cantidad = String.format("%.2f", cant);
                } catch (NumberFormatException e) {
                    // Si no es número, intentar extraer números de string
                    String numericOnly = cantidad.replaceAll("[^0-9.]", "");
                    try {
                        double cant = Double.parseDouble(numericOnly);
                        cantidad = String.format("%.2f", cant);
                    } catch (NumberFormatException e2) {
                        cantidad = "1.00";
                    }
                }

                // Obtener unidad
                String unidad = item.has("unidad") ? item.path("unidad").asText() : "und";
                if (unidad == null || unidad.isEmpty()) {
                    unidad = item.has("unit") ? item.path("unit").asText() : "und";
                }

                // Obtener justificación
                String justificacion = item.has("justificacion") ? item.path("justificacion").asText() :
                        (item.has("justification") ? item.path("justification").asText() : "Estimado por IA");

                resultado.put("nombre", nombre);
                resultado.put("cantidadEstimada", cantidad);
                resultado.put("unidad", unidad);
                resultado.put("justificacion", justificacion);

                System.out.println("📊 " + nombre + ": " + cantidad + " " + unidad);
                resultados.add(resultado);
            }

            return resultados;
        } catch (Exception e) {
            System.out.println("❌ Error parseando cantidades Gemini: " + e.getMessage());
            throw new Exception("Formato de respuesta inválido");
        }
    }

    private String limpiarRespuesta(String respuesta) {
        String json = respuesta.trim();
        json = json.replace("```json", "").replace("```", "").trim();
        return json;
    }

}
