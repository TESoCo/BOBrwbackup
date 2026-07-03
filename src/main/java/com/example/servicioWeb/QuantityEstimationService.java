package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuantityEstimationService {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private OpenRouterService openRouterService;

    @Autowired
    private OllamaService ollamaService;

    /**
     * Estima cantidades usando todos los servicios disponibles
     */
    public List<Map<String, String>> estimarCantidades(
            List<Map<String, String>> materiales,
            String descripcionApu,
            String unidadBase) {

        System.out.println("=== ESTIMACIÓN DE CANTIDADES ===");
        System.out.println("Materiales: " + materiales.size());
        System.out.println("Unidad base: " + unidadBase);

        if (materiales == null || materiales.isEmpty()) {
            return new ArrayList<>();
        }

        // LIMPIAR MATERIALES - Remover campos que confunden a la IA
        List<Map<String, String>> materialesLimpios = new ArrayList<>();
        for (Map<String, String> material : materiales) {
            Map<String, String> materialLimpio = new HashMap<>();
            materialLimpio.put("nombre", material.get("nombre"));
            materialLimpio.put("descripcion", material.get("descripcion"));
            materialLimpio.put("unidad", material.get("unidad"));
            // NO incluir precio ni proveedor para que la IA no los confunda con cantidad
            materialesLimpios.add(materialLimpio);
        }

        System.out.println("Materiales limpiados (sin precio): " + materialesLimpios.size());

        // Intentar con cada servicio en orden de prioridad
        List<Map<String, String>> resultado = null;

        // 1. Gemini
        try {
            System.out.println("🔄 Intentando Gemini...");
            resultado = geminiService.estimarCantidades(descripcionApu, materiales, unidadBase);
            if (resultado != null && !resultado.isEmpty()) {
                System.out.println("Estimación Gemini exitosa: " + resultado.size() + " materiales");
                // Verificar que las cantidades no sean cero
                boolean tieneCantidades = resultado.stream().anyMatch(m -> {
                    String cant = m.get("cantidadEstimada");
                    return cant != null && !cant.isEmpty() && !"0".equals(cant) && !"0.0".equals(cant);
                });
                if (tieneCantidades) {
                    return resultado;
                } else {
                    System.out.println("⚠️ Gemini devolvió todas las cantidades en cero, probando otro servicio");
                }

            }
        } catch (Exception e) {
            System.out.println("❌ Gemini falló: " + e.getMessage());
        }

        // 2. OpenRouter
        try {
            System.out.println("🔄 Intentando OpenRouter...");
            resultado = openRouterService.estimarCantidades(descripcionApu, materiales, unidadBase);
            if (resultado != null && !resultado.isEmpty()) {
                System.out.println("Estimación OpenRouter exitosa: " + resultado.size() + " materiales");
                boolean tieneCantidades = resultado.stream().anyMatch(m -> {
                    String cant = m.get("cantidadEstimada");
                    return cant != null && !cant.isEmpty() && !"0".equals(cant) && !"0.0".equals(cant);
                });
                if (tieneCantidades) {
                    return resultado;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ OpenRouter falló: " + e.getMessage());
        }

        // 3. DeepSeek
        try {
            System.out.println("🔄 Intentando DeepSeek...");
            resultado = deepSeekService.estimarCantidades(descripcionApu, materiales, unidadBase);
            if (resultado != null && !resultado.isEmpty()) {
                System.out.println("Estimación DeepSeek exitosa: " + resultado.size() + " materiales");
                boolean tieneCantidades = resultado.stream().anyMatch(m -> {
                    String cant = m.get("cantidadEstimada");
                    return cant != null && !cant.isEmpty() && !"0".equals(cant) && !"0.0".equals(cant);
                });
                if (tieneCantidades) {
                    return resultado;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ DeepSeek falló: " + e.getMessage());
        }

        // 4. Ollama
        try {
            System.out.println("🔄 Intentando Ollama...");
            resultado = ollamaService.estimarCantidades(descripcionApu, materiales, unidadBase);
            if (resultado != null && !resultado.isEmpty()) {
                System.out.println("Estimación Ollama exitosa: " + resultado.size() + " materiales");
                boolean tieneCantidades = resultado.stream().anyMatch(m -> {
                    String cant = m.get("cantidadEstimada");
                    return cant != null && !cant.isEmpty() && !"0".equals(cant) && !"0.0".equals(cant);
                });
                if (tieneCantidades) {
                    return resultado;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Ollama falló: " + e.getMessage());
        }

        // Fallback manual
        System.out.println("📐 Usando estimación manual");
        return estimarCantidadesManual(materiales, descripcionApu, unidadBase);
    }

    /**
     * Estimación manual como fallback
     */
    private List<Map<String, String>> estimarCantidadesManual(
            List<Map<String, String>> materiales,
            String descripcionApu,
            String unidadBase) {

        List<Map<String, String>> resultado = new ArrayList<>();

        for (Map<String, String> material : materiales) {
            Map<String, String> materialConCantidad = new HashMap<>(material);
            String nombre = material.get("nombre").toLowerCase();

            double cantidad = 1.0;

            if (unidadBase.contains("m²") || unidadBase.contains("m2")) {
                if (nombre.contains("cemento") || nombre.contains("concreto")) cantidad = 100.0;
                else if (nombre.contains("arena") || nombre.contains("grava")) cantidad = 1.5;
                else if (nombre.contains("ladrillo") || nombre.contains("bloque")) cantidad = 50.0;
                else if (nombre.contains("pintura")) cantidad = 4.0;
            } else if (unidadBase.contains("m³") || unidadBase.contains("m3")) {
                if (nombre.contains("cemento")) cantidad = 300.0;
                else if (nombre.contains("arena") || nombre.contains("grava")) cantidad = 0.6;
                else if (nombre.contains("gavion") || nombre.contains("piedra")) cantidad = 1.5;
            } else {
                if (nombre.contains("cemento")) cantidad = 50.0;
                else if (nombre.contains("arena") || nombre.contains("grava")) cantidad = 0.8;
                else if (nombre.contains("ladrillo") || nombre.contains("bloque")) cantidad = 20.0;
                else if (nombre.contains("malla") || nombre.contains("geotextil")) cantidad = 10.0;
                else if (nombre.contains("tubo") || nombre.contains("perfil")) cantidad = 6.0;
            }

            materialConCantidad.put("cantidadEstimada", String.format("%.2f", cantidad));
            materialConCantidad.put("justificacion", "Estimación manual");
            resultado.add(materialConCantidad);
        }

        return resultado;
    }
}