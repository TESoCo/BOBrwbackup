package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 segundos
        factory.setReadTimeout(30000);    // 30 segundos
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    // ============================================
    // MÉTODOS PÚBLICOS
    // ============================================

    /**
     * Genera materiales desde descripción
     */

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== DEEPSEEK ===");
        System.out.println("API Key configurada: " + (apiKey != null && !apiKey.isEmpty()));
        System.out.println("API URL: " + apiUrl);

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("tu_api_key_aqui")) {
            System.out.println("API key no configurada - Verifica las variables de entorno");
            throw new RuntimeException("Error al generar materiales" );
        }

        try {
            System.out.println("Llamando API DeepSeek gratuita...");
            String prompt = crearPromptMateriales(descripcionApu);
            String respuesta = llamarDeepSeekAPI(prompt);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);
            System.out.println("Materiales generados por IA: " + materiales.size());
            return materiales;

        } catch (Exception e) {
            System.out.println("Error con API DeepSeek: " + e.getMessage());

            throw new RuntimeException("Error al generar materiales: " + e.getMessage());
        }
    }

    /**
     * Estima cantidades
     */
    public List<Map<String, String>> estimarCantidades(String descripcionApu, List<Map<String, String>> materiales, String unidadBase) {
        System.out.println("=== DEEPSEEK - ESTIMACIÓN DE CANTIDADES ===");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("tu_api_key_aqui")) {
            throw new RuntimeException("API Key de DeepSeek no configurada");
        }

        try {
            String prompt = crearPromptCantidades(descripcionApu, materiales, unidadBase);
            String respuesta = llamarDeepSeekAPI(prompt);
            return parsearRespuestaCantidades(respuesta);
        } catch (Exception e) {
            System.out.println("❌ Error DeepSeek estimación: " + e.getMessage());
            throw new RuntimeException("Error al estimar cantidades: " + e.getMessage());
        }
    }

    // ============================================
    // MÉTODOS PRIVADOS DE PROMPT
    // ============================================

    /**
     * Crea prompt para generación de materiales
     */
    private String crearPromptMateriales(String descripcionApu) {
        return "Eres un experto en construcción. Analiza esta descripción y genera una lista de materiales necesarios. " +
                "Responde SOLO con un array JSON. Cada objeto debe tener: nombre, descripcion, unidad, precio, proveedor.\n\n" +
                "Unidades permitidas: m³, m², m, kg, und, gl, l, hr, día\n\n" +
                "Descripción: " + descripcionApu + "\n\n" +
                "Ejemplo: [{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para construcción\", \"unidad\": \"kg\", \"precio\": \"10000.00\", \"proveedor\": \"Alkosto\"}]";
    }

    /**
     * Crea prompt para estimación de cantidades
     */
    private String crearPromptCantidades(String descripcionApu, List<Map<String, String>> materiales, String unidadBase) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un ingeniero civil experto en construcción y presupuestos.\n\n");
        sb.append("ACTIVIDAD A REALIZAR (para 1 UNIDAD del APU):\n");
        sb.append(descripcionApu).append("\n\n");
        sb.append("UNIDAD DE MEDIDA BASE DEL APU: ").append(unidadBase).append("\n\n");
        sb.append("MATERIALES NECESARIOS:\n");

        for (Map<String, String> material : materiales) {
            sb.append("- ").append(material.get("nombre"));
            sb.append(" (").append(material.get("unidad")).append("): ");
            sb.append(material.get("descripcion")).append("\n");
        }

        sb.append("""

            INSTRUCCIONES:
            1. Estima las CANTIDADES FÍSICAS requeridas de cada material para realizar 1 UNIDAD de la actividad.
            2. NO confundas cantidad con precio.
            3. Considera desperdicios (10-15%) y factores de seguridad.
            4. Responde SOLO con un array JSON: nombre, cantidad (número), unidad, justificacion (opcional).

            EJEMPLO:
            [
              {"nombre": "Cemento", "cantidad": 45.50, "unidad": "kg", "justificacion": "Para 2 m³ de concreto"}
            ]

            AHORA GENERA TU RESPUESTA (SOLO JSON):
            """);

        return sb.toString();
    }

    private String llamarDeepSeekAPI(String prompt) throws Exception {


        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2000);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                return content;
            } else {
                throw new Exception("Error HTTP: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new Exception("Límite de tasa excedido - espera un momento");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new Exception("API key inválida o expirada");
            } else {
                throw new Exception("Error de API: " + e.getMessage());
            }
        }
    }

    // ============================================
    // MÉTODS DE PARSING
    // ============================================

    /**
     * Parsea respuesta para materiales
     */

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            // Limpiar la respuesta por si hay texto alrededor del JSON
            respuesta = respuesta.trim();
            if (respuesta.startsWith("```json")) {
                respuesta = respuesta.substring(7);
            }
            if (respuesta.endsWith("```")) {
                respuesta = respuesta.substring(0, respuesta.length() - 3);
            }

            JsonNode materialesArray = objectMapper.readTree(respuesta);
            List<Map<String, String>> materiales = new ArrayList<>();

            for (JsonNode materialNode : materialesArray) {
                Map<String, String> material = new HashMap<>();
                material.put("nombre", materialNode.path("nombre").asText());
                material.put("descripcion", materialNode.path("descripcion").asText());
                material.put("unidad", materialNode.path("unidad").asText());
                material.put("precio", "1000.00");
                material.put("proveedor", "");
                materiales.add(material);
            }

            return materiales;
        } catch (Exception e) {
            System.out.println("Error parseando respuesta JSON: " + e.getMessage());
            System.out.println("Respuesta recibida: " + respuesta);
            throw new Exception("Formato de respuesta inválido");
        }
    }

    /**
     * Parsea respuesta para cantidades - NUEVO
     */
    private List<Map<String, String>> parsearRespuestaCantidades(String respuesta) throws Exception {
        try {
            String json = limpiarRespuesta(respuesta);
            JsonNode array = objectMapper.readTree(json);
            List<Map<String, String>> resultados = new ArrayList<>();

            for (JsonNode item : array) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("nombre", item.path("nombre").asText());
                resultado.put("cantidadEstimada", item.path("cantidad").asText());
                resultado.put("unidad", item.path("unidad").asText());
                resultado.put("justificacion", item.path("justificacion").asText("Estimado por IA"));
                resultados.add(resultado);
            }
            return resultados;
        } catch (Exception e) {
            System.out.println("Error parseando JSON de cantidades: " + e.getMessage());
            throw new Exception("Formato de respuesta inválido");
        }
    }

    /**
     * Limpia la respuesta de la IA
     */
    private String limpiarRespuesta(String respuesta) {
        String json = respuesta.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }



    private Map<String, String> crearMaterial(String nombre, String descripcion, String unidad) {
        Map<String, String> material = new HashMap<>();
        material.put("nombre", nombre);
        material.put("descripcion", descripcion);
        material.put("unidad", unidad);
        material.put("precio", "1000.00");
        material.put("proveedor", "");
        return material;
    }
}