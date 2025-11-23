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

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== GEMINI SERVICE ===");
        System.out.println("Descripción: " + descripcionApu);

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("tu_api_key")) {
            System.out.println("❌ Gemini API Key no configurada");
            throw new RuntimeException("API Key de Gemini no configurada");
        }

        try {
            System.out.println("🔄 Llamando a Gemini API...");
            String respuesta = llamarGeminiAPI(descripcionApu);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);
            System.out.println("✅ Gemini exitoso: " + materiales.size() + " materiales");
            return materiales;

        } catch (Exception e) {
            System.out.println("❌ Error Gemini: " + e.getMessage());
            throw new RuntimeException("Error al generar materiales con Gemini: " + e.getMessage());
        }
    }

    private String llamarGeminiAPI(String descripcion) throws Exception {
        String prompt = crearPrompt(descripcion);

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

        System.out.println("📤 Enviando petición a Gemini...");

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

    private String crearPrompt(String descripcionApu) {
        return "Eres un experto en construcción. Analiza esta descripción y genera una lista de materiales necesarios. " +
                "Responde SOLO con un array JSON válido. Cada objeto debe tener exactamente: nombre, descripcion, unidad.\n\n" +
                "Unidades permitidas: m³, m², m, kg, und, gl, l, hr, día, viaje, juego\n\n" +
                "Descripción: " + descripcionApu + "\n\n" +
                "Ejemplo de formato:\n" +
                "[{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para construcción\", \"unidad\": \"kg\"}]\n\n" +
                "Genera entre 5-8 materiales relevantes para la actividad descrita.";
    }

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            // Limpiar la respuesta
            String json = respuesta.trim();
            json = json.replace("```json", "").replace("```", "").trim();

            System.out.println("🔧 Parseando respuesta Gemini: " + json.substring(0, Math.min(100, json.length())) + "...");

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
            System.out.println("❌ Error parseando respuesta Gemini: " + e.getMessage());
            System.out.println("Respuesta cruda: " + respuesta);
            throw new Exception("Gemini no respondió en formato JSON válido");
        }
    }
}
