package com.example.servicioWeb;

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
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== DEEPSEEK SERVICE GRATUITO ===");

        // Si no hay API key configurada, usar método local
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("tu_api_key_aqui")) {
            System.out.println("API key no configurada - usando generador local");
            return generarMaterialesLocalmente(descripcionApu);
        }

        try {
            System.out.println("Llamando API DeepSeek gratuita...");
            String respuesta = llamarDeepSeekAPI(descripcionApu);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);
            System.out.println("Materiales generados por IA: " + materiales.size());
            return materiales;

        } catch (Exception e) {
            System.out.println("Error con API DeepSeek: " + e.getMessage());
            System.out.println("Usando generador local como fallback");
            return generarMaterialesLocalmente(descripcionApu);
        }
    }

    private String llamarDeepSeekAPI(String descripcion) throws Exception {
        String prompt = crearPrompt(descripcion);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
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
                return root.path("choices").get(0).path("message").path("content").asText();
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

    private String crearPrompt(String descripcionApu) {
        return "Eres un experto en construcción. Analiza esta descripción y genera una lista de materiales necesarios. " +
                "Responde SOLO con un array JSON. Cada objeto debe tener: nombre, descripcion, unidad.\n\n" +
                "Unidades permitidas: m³, m², m, kg, und, gl, l, hr, día\n\n" +
                "Descripción: " + descripcionApu + "\n\n" +
                "Ejemplo: [{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para construcción\", \"unidad\": \"kg\"}]";
    }

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
                materiales.add(material);
            }

            return materiales;
        } catch (Exception e) {
            System.out.println("Error parseando respuesta JSON: " + e.getMessage());
            System.out.println("Respuesta recibida: " + respuesta);
            throw new Exception("Formato de respuesta inválido");
        }
    }

    private List<Map<String, String>> generarMaterialesLocalmente(String descripcion) {
        System.out.println("Usando generador local para: " + descripcion);

        List<Map<String, String>> materiales = new ArrayList<>();
        String descLower = descripcion.toLowerCase();

        // Lógica local mejorada
        if (descLower.contains("columna") || descLower.contains("concreto") || descLower.contains("fundición")) {
            materiales.add(crearMaterial("Cemento gris", "Cemento para construcción general", "kg"));
            materiales.add(crearMaterial("Arena lavada", "Arena fina para mezclas", "m³"));
            materiales.add(crearMaterial("Grava triturada", "Grava de 1/2\" para concretos", "m³"));
            materiales.add(crearMaterial("Varilla 1/2\"", "Varilla corrugada para refuerzo principal", "m"));
            materiales.add(crearMaterial("Varilla 3/8\"", "Varilla para estribos y amarre", "m"));
            materiales.add(crearMaterial("Madera", "Madera para formaleta", "m²"));
            materiales.add(crearMaterial("Alambre de amarre", "Alambre negro para amarre", "kg"));
        }

        if (materiales.isEmpty()) {
            materiales.add(crearMaterial("Material de construcción", "Material básico requerido", "und"));
        }

        return materiales;
    }

    private Map<String, String> crearMaterial(String nombre, String descripcion, String unidad) {
        Map<String, String> material = new HashMap<>();
        material.put("nombre", nombre);
        material.put("descripcion", descripcion);
        material.put("unidad", unidad);
        return material;
    }
}