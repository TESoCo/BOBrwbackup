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

    @Value("${openrouter.api.model:meta-llama/llama-3.1-8b-instruct:free}")
    private String model;

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
        System.out.println("Modelo: " + model);
        System.out.println("Descripción: " + descripcionApu);

        // Verificar API Key
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("tu_api_key")) {
            System.out.println("❌ API Key no configurada - Verifica application.properties");
            throw new RuntimeException("API Key de OpenRouter no configurada");
        }

        try {
            System.out.println("🔄 Llamando a OpenRouter API...");
            String respuesta = llamarAPI(descripcionApu);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);
            System.out.println("✅ Materiales generados por IA: " + materiales.size());
            return materiales;

        } catch (Exception e) {
            System.out.println("❌ Error OpenRouter: " + e.getMessage());
            throw new RuntimeException("Error al generar materiales: " + e.getMessage());
        }
    }

    private String llamarAPI(String descripcion) throws Exception {
        // Construir el prompt más específico
        String prompt = "Eres un experto en construcción. Analiza esta descripción y genera materiales en formato JSON.\n\n" +
                "ACTIVIDAD: " + descripcion + "\n\n" +
                "RESPONDE SOLO CON JSON ARRAY. Ejemplo:\n" +
                "[{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris\", \"unidad\": \"kg\"}]\n\n" +
                "Genera 5-7 materiales relevantes.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
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
        System.out.println("🔧 Usando modelo: " + model);

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
            // Limpiar la respuesta
            String json = respuesta.trim();
            json = json.replace("```json", "").replace("```", "").trim();

            System.out.println("🔧 Parseando JSON: " + json);

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
            System.out.println("Respuesta cruda: " + respuesta);
            throw new Exception("La IA no respondió en formato JSON válido");
        }
    }
}