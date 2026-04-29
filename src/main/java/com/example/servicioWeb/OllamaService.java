package com.example.servicioWeb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OllamaService {

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:phi:2.7b-mini-q4_K_M}")
    private String modelName;

    @Value("${ollama.enabled:true}")
    private boolean enabled;

    @Value("${ollama.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, String>> generarMaterialesDesdeDescripcion(String descripcionApu) {
        System.out.println("=== OLLAMA SERVICE (LOCAL LLM) ===");
        System.out.println("Modelo: " + modelName);
        System.out.println("URL: " + ollamaUrl);

        if (!enabled) {
            System.out.println("⚠️ Ollama está deshabilitado en configuración");
            throw new RuntimeException("Ollama deshabilitado");
        }

        try {
            System.out.println("🔄 Verificando conexión con Ollama...");
            if (!verificarConexion()) {
                throw new RuntimeException("No se pudo conectar a Ollama en " + ollamaUrl);
            }

            System.out.println("🔄 Generando materiales con modelo local...");
            String respuesta = llamarOllamaAPI(descripcionApu);
            List<Map<String, String>> materiales = parsearRespuesta(respuesta);

            if (materiales.isEmpty()) {
                throw new RuntimeException("No se generaron materiales");
            }

            System.out.println("✅ Ollama exitoso: " + materiales.size() + " materiales");
            return materiales;

        } catch (ResourceAccessException e) {
            System.out.println("❌ No se pudo conectar a Ollama: " + e.getMessage());
            throw new RuntimeException("Ollama no disponible: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error con Ollama: " + e.getMessage());
            throw new RuntimeException("Error al generar materiales con Ollama: " + e.getMessage());
        }
    }

    private boolean verificarConexion() {
        try {
            String url = ollamaUrl + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean conectado = response.getStatusCode() == HttpStatus.OK;
            if (conectado) {
                System.out.println("✅ Conexión a Ollama establecida");
                // Listar modelos disponibles
                try {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode models = root.path("models");
                    if (models.isArray() && !models.isEmpty()) {
                        System.out.println("📦 Modelos disponibles en Ollama:");
                        for (JsonNode model : models) {
                            System.out.println("   - " + model.path("name").asText());
                        }
                    }
                } catch (Exception e) {
                    // No importa si no se pueden listar
                }
            }
            return conectado;
        } catch (Exception e) {
            System.out.println("❌ No se pudo verificar conexión a Ollama: " + e.getMessage());
            return false;
        }
    }

    private String llamarOllamaAPI(String descripcion) throws Exception {
        String prompt = crearPrompt(descripcion);
        String url = ollamaUrl + "/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1500);
        requestBody.put("num_predict", 1500);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        System.out.println("📤 Enviando petición a Ollama...");
        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("⏱️ Respuesta recibida en " + elapsed + "ms");

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String respuesta = root.path("response").asText();
            if (respuesta == null || respuesta.isEmpty()) {
                throw new Exception("Respuesta vacía del modelo");
            }
            return respuesta;
        } else {
            throw new Exception("Error HTTP: " + response.getStatusCode());
        }
    }

    private String crearPrompt(String descripcionApu) {
        return """
            Eres un experto en construcción civil. Genera una lista de materiales necesarios para la siguiente actividad.
            
            DESCRIPCIÓN: """ + descripcionApu + """
            
            INSTRUCCIONES:
            1. Responde SOLO con un array JSON válido
            2. Cada objeto debe tener: nombre, descripcion, unidad
            3. Genera entre 4 y 8 materiales
            4. Unidades permitidas: m³, m², m, kg, und, gl, l, hr
            
            FORMATO DE RESPUESTA (SOLO EL JSON, sin texto extra):
            [
              {"nombre": "Cemento", "descripcion": "Cemento gris para construcción", "unidad": "kg"},
              {"nombre": "Arena", "descripcion": "Arena lavada para mezcla", "unidad": "m³"}
            ]
            
            AHORA GENERA TU RESPUESTA (SOLO JSON):
            """;
    }

    private List<Map<String, String>> parsearRespuesta(String respuesta) throws Exception {
        try {
            String json = respuesta.trim();

            // Limpiar markdown y texto extra
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
            }
            if (json.contains("```")) {
                json = json.substring(0, json.lastIndexOf("```"));
            }
            json = json.replace("```json", "").replace("```", "").trim();

            // Buscar el array JSON
            int firstBracket = json.indexOf('[');
            int lastBracket = json.lastIndexOf(']');
            if (firstBracket >= 0 && lastBracket > firstBracket) {
                json = json.substring(firstBracket, lastBracket + 1);
            }

            // Limpiar caracteres problemáticos
            json = json.replace("\n", " ")
                    .replace("\r", " ")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replaceAll(",\\s*}", "}")
                    .replaceAll(",\\s*]", "]")
                    .trim();

            System.out.println("🔧 JSON limpio: " + (json.length() > 200 ? json.substring(0, 200) + "..." : json));

            JsonNode array = objectMapper.readTree(json);
            List<Map<String, String>> materiales = new ArrayList<>();

            for (JsonNode item : array) {
                Map<String, String> material = new HashMap<>();

                String nombre = item.path("nombre").asText();
                String descripcion = item.path("descripcion").asText();
                String unidad = item.path("unidad").asText();

                // Validar y limpiar valores
                if (nombre == null || nombre.isEmpty()) continue;

                material.put("nombre", nombre.trim());
                material.put("descripcion", (descripcion != null && !descripcion.isEmpty()) ? descripcion.trim() : nombre.trim());
                material.put("unidad", (unidad != null && !unidad.isEmpty()) ? unidad.trim() : "und");

                materiales.add(material);
            }

            return materiales;
        } catch (Exception e) {
            System.out.println("❌ Error parseando respuesta Ollama: " + e.getMessage());
            System.out.println("Respuesta cruda: " + (respuesta.length() > 500 ? respuesta.substring(0, 500) + "..." : respuesta));
            throw new Exception("La respuesta de Ollama no es un JSON válido");
        }
    }

    // Método para probar la conexión con diferentes modelos
    public Map<String, Object> probarModelos() {
        Map<String, Object> resultado = new HashMap<>();
        List<String> modelosPrueba = Arrays.asList(
                "phi:2.7b-mini-q4_K_M",
                "tinyllama:latest",
                "qwen2.5:1.5b",
                "gemma2:2b",
                "llama3.2:1b"
        );

        Map<String, Boolean> disponibles = new HashMap<>();
        for (String modelo : modelosPrueba) {
            try {
                String url = ollamaUrl + "/api/generate";
                Map<String, Object> request = new HashMap<>();
                request.put("model", modelo);
                request.put("prompt", "Hola");
                request.put("stream", false);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, new HttpHeaders());
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                disponibles.put(modelo, response.getStatusCode() == HttpStatus.OK);
            } catch (Exception e) {
                disponibles.put(modelo, false);
            }
        }

        resultado.put("conectado", verificarConexion());
        resultado.put("modelo_actual", modelName);
        resultado.put("modelos_disponibles", disponibles);
        return resultado;
    }
}