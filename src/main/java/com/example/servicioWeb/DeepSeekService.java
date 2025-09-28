package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        try {
            String prompt = crearPrompt(descripcionApu);
            String respuesta = llamarAPI(prompt);
            return parsearRespuesta(respuesta);
        } catch (Exception e) {
            // Fallback: retornar lista básica si falla la API
            return crearListaBasica(descripcionApu);
        }
    }

    private String crearPrompt(String descripcionApu) {
        return "Analiza la siguiente descripción de actividad de construcción y genera una lista de materiales necesarios. " +
                "Responde SOLO en formato JSON con un array de objetos, cada objeto debe tener: " +
                "'nombre' (nombre del material), 'descripcion' (descripción breve), 'unidad' (unidad de medida: m³, m², m, kg, und, gl, l, hr, día), 'cantidadEstimada' (número estimado).\n\n" +
                "Descripción: " + descripcionApu + "\n\n" +
                "Ejemplo de formato esperado: [{\"nombre\": \"Cemento\", \"descripcion\": \"Cemento gris para construcción\", \"unidad\": \"kg\", \"cantidadEstimada\": 50}]";
    }

    private String llamarAPI(String prompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("API key no configurada");
        }

        String url = "https://api.deepseek.com/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } else {
            throw new Exception("Error en la API: " + response.getStatusCode());
        }
    }

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            JsonNode materialesArray = objectMapper.readTree(respuesta);
            List<Map<String, String>> materiales = new ArrayList<>();

            for (JsonNode materialNode : materialesArray) {
                Map<String, String> material = new HashMap<>();
                material.put("nombre", materialNode.path("nombre").asText());
                material.put("descripcion", materialNode.path("descripcion").asText());
                material.put("unidad", materialNode.path("unidad").asText());
                material.put("cantidadEstimada", materialNode.path("cantidadEstimada").asText());
                materiales.add(material);
            }

            return materiales;
        } catch (Exception e) {
            // Si falla el parsing, crear lista básica
            return crearListaBasica("Materiales para construcción");
        }
    }

    private List<Map<String, String>> crearListaBasica(String descripcion) {
        List<Map<String, String>> materialesBasicos = new ArrayList<>();

        // Materiales básicos de construcción
        String[] materiales = {
                "Cemento gris, Cemento para construcción general, kg, 50",
                "Arena lavada, Arena fina para mezclas, m³, 0.1",
                "Grava triturada, Grava de 1/2 para concretos, m³, 0.1",
                "Varilla corrugada, Varilla de 1/2 para refuerzo, m, 20",
                "Madera, Madera para encofrados, m², 5"
        };

        for (String materialStr : materiales) {
            String[] partes = materialStr.split(", ");
            if (partes.length == 4) {
                Map<String, String> material = new HashMap<>();
                material.put("nombre", partes[0]);
                material.put("descripcion", partes[1]);
                material.put("unidad", partes[2]);
                material.put("cantidadEstimada", partes[3]);
                materialesBasicos.add(material);
            }
        }

        return materialesBasicos;
    }


}
