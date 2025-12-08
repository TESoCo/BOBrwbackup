package com.example.servicioWeb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    // Lista de modelos a intentar en orden
    private final String[] modelos = {
            "google/gemini-2.0-flash-exp:free",        // 1er intento
            "meta-llama/llama-3.2-3b-instruct:free",   // 2do intento
            "huggingfaceh4/zephyr-7b-beta:free",  // 3er intento
            "mistralai/mistral-7b-instruct:free",      // 4to intento
            "openai/gpt-4o-mini",                       // 5to
            "microsoft/phi-3-medium-128k-instruct:free"  // <-- NUEVO
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterService() {
        this.restTemplate = crearRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate crearRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000); // 15 segundos
        factory.setReadTimeout(30000); // 30 segundos
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== OPENROUTER SERVICE ===");
        System.out.println("Intentando con " + modelos.length + " modelos...");
        System.out.println("Descripción: " + descripcionApu);

        // Intentar con cada modelo hasta que uno funcione
        for (int i = 0; i < modelos.length; i++) {
            String modeloActual = modelos[i];
            try {
                System.out.println("\n🔄 Intento " + (i+1) + "/" + modelos.length);
                System.out.println("Modelo: " + modeloActual);

                String respuesta = llamarAPI(descripcionApu, modeloActual);
                List<Map<String, String>> materiales = parsearRespuesta(respuesta);

                System.out.println("✅ Éxito con modelo: " + modeloActual);
                System.out.println("📦 Materiales generados: " + materiales.size());
                return materiales;

            } catch (Exception e) {
                System.out.println("❌ Falló modelo " + modeloActual + ": " +
                        (e.getMessage().length() > 100 ? e.getMessage().substring(0, 100) + "..." : e.getMessage()));

                // Esperar un poco entre intentos
                if (i < modelos.length - 1) {
                    try {
                        Thread.sleep(1000); // 1 segundo entre intentos
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        throw new RuntimeException("Todos los modelos de OpenRouter fallaron");
    }

    private String llamarAPI(String descripcion, String modelo) throws Exception {
        // Construir el prompt más específico
        String prompt = "Eres un asistente experto en construcción civil. Necesito que generes una lista de materiales en formato JSON.\n\n" +
                "DESCRIPCIÓN DE LA ACTIVIDAD: " + descripcion + "\n\n" +
                "INSTRUCCIONES:\n" +
                "1. Genera SOLO un array JSON\n" +
                "2. Cada elemento debe tener: nombre, descripcion, unidad\n" +
                "3. Genera entre 5 y 9 materiales\n" +
                "4. La unidad debe ser: kg, m³, m², m, gl, und (abreviado)\n" +
                "5. NO agregues explicaciones ni texto extra\n" +
                "6. NO incluyas etiquetas como ```json\n\n" +
                "EJEMPLO DE RESPUESTA VÁLIDA:\n" +
                "[{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para mezcla\", \"unidad\": \"kg\"}]\n\n" +
                "RESPONDE SOLO CON EL ARRAY JSON:";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelo);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1500);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "BOB Construction");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        System.out.println("📤 Enviando petición a: " + apiUrl);
        System.out.println("🔧 Usando modelo: " + modelo);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("✅ Respuesta recibida exitosamente");
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } else {
            throw new Exception("Error " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            // LIMPIAR MÁS AGRESIVAMENTE
            String json = respuesta.trim();

            // Eliminar tags XML/HTML
            json = json.replace("<s>", "")
                    .replace("</s>", "")
                    .replace("[INST]", "")
                    .replace("[/INST]", "")
                    .trim();

            // Eliminar markdown code blocks
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
            }
            if (json.contains("```")) {
                json = json.substring(0, json.lastIndexOf("```"));
            }
            json = json.replace("```json", "").replace("```", "").trim();

            // Eliminar comentarios o texto antes del JSON
            int firstBracket = json.indexOf('[');
            if (firstBracket > 0) {
                json = json.substring(firstBracket);
            }

            // Eliminar texto después del JSON
            int lastBracket = json.lastIndexOf(']');
            if (lastBracket > 0 && lastBracket < json.length() - 1) {
                json = json.substring(0, lastBracket + 1);
            }

            // Eliminar saltos de línea y espacios extra
            json = json.replace("\n", " ").replace("\r", " ").trim();

            System.out.println("🔧 JSON limpio: " + (json.length() > 100 ? json.substring(0, 100) + "..." : json));

            JsonNode array = objectMapper.readTree(json);
            List<Map<String, String>> materiales = new ArrayList<>();

            for (JsonNode item : array) {
                Map<String, String> material = new HashMap<>();
                material.put("nombre", item.path("nombre").asText());
                material.put("descripcion", item.path("descripcion").asText());
                material.put("unidad", item.path("unidad").asText());
                materiales.add(material);
                System.out.println("📦 " + material.get("nombre") + " (" + material.get("unidad") + ")");
            }

            return materiales;
        } catch (Exception e) {
            System.out.println("❌ Error parseando JSON: " + e.getMessage());
            System.out.println("Respuesta cruda completa:");
            System.out.println(respuesta);
            throw new Exception("La IA no respondió en formato JSON válido");
        }
    }
}