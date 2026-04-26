package com.example.servicioWeb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private OpenRouterService openRouterService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private CacheService cacheService;

    public List<Map<String, String>> generarMateriales(String descripcion) {

        // 1. Verificar caché
        String hash = cacheService.generarHash(descripcion);
        List<Map<String, String>> cacheado = cacheService.obtenerDelCache(hash);

        if (cacheado != null && !cacheado.isEmpty()) {
            System.out.println("🎯 Usando respuesta cacheada (ahorro de API call)");
            return cacheado;
        }
        // 2. Intentar APIs
        // Intentar OpenRouter primero
        try {
            System.out.println("Intentando OpenRouter...");
            List<Map<String, String>> materiales = openRouterService.generarMaterialesDesdeDescripcion(descripcion);
            System.out.println("✅ OpenRouter exitoso: " + materiales.size() + " materiales");
            cacheService.guardarEnCache(hash, materiales);
            return materiales;
        } catch (Exception e) {
            System.out.println("❌ OpenRouter falló: " + e.getMessage());
        }



        // 2° Intento: Gemini (nueva opción)
        try {
            System.out.println("🔄 2° Intentando Gemini...");
            List<Map<String, String>> materiales = geminiService.generarMaterialesDesdeDescripcion(descripcion);
            System.out.println("✅ Gemini exitoso: " + materiales.size() + " materiales");
            cacheService.guardarEnCache(hash, materiales);
            return materiales;
        } catch (Exception e) {
            System.out.println("❌ Gemini falló: " + e.getMessage());
        }



        // Fallback a DeepSeek
        try {
            System.out.println("Intentando DeepSeek...");
            List<Map<String, String>> materiales = deepSeekService.generarMaterialesDesdeDescripcion(descripcion);
            System.out.println("✅ DeepSeek exitoso: " + materiales.size() + " materiales");
            cacheService.guardarEnCache(hash, materiales);
            return materiales;

        } catch (Exception e) {
            System.out.println("❌ DeepSeek falló: " + e.getMessage());
        }

        // Último recurso: generador local
        System.out.println("🔄 Usando generador local...");
        return generarMaterialesLocalmente(descripcion);
    }

    public List<Map<String, String>> generarMaterialesLocalmente(String descripcion) {
        System.out.println("🔧 Generando materiales localmente para: " + descripcion);

        // 1. Verificar caché
        String hash = cacheService.generarHash(descripcion);
        List<Map<String, String>> cacheado = cacheService.obtenerDelCache(hash);

        if (cacheado != null && !cacheado.isEmpty()) {
            System.out.println("🎯 Usando respuesta cacheada (ahorro de API call)");
            return cacheado;
        }

        List<Map<String, String>> materiales = new ArrayList<>();
        String descLower = descripcion.toLowerCase();

        // Lógica local mejorada basada en palabras clave
        if (descLower.contains("gavion") || descLower.contains("contención") || descLower.contains("piedra") || descLower.contains("malla")) {
            materiales.add(crearMaterial("Piedra para gavión", "Piedra de 5 a 10 pulgadas para relleno de gaviones", "m³"));
            materiales.add(crearMaterial("Malla galvanizada", "Malla calibre 12 para estructura de gaviones", "m²"));
            materiales.add(crearMaterial("Geotextil", "Geotextil NT 1800 para filtro y separación", "m²"));
            materiales.add(crearMaterial("Alambre de amarre", "Alambre galvanizado para unión de mallas", "kg"));
            materiales.add(crearMaterial("Estacas de madera", "Estacas para fijación y alineación", "und"));
        } else if (descLower.contains("concreto") || descLower.contains("hormigón") || descLower.contains("columna") || descLower.contains("viga")) {
            materiales.add(crearMaterial("Cemento gris", "Cemento para construcción general", "kg"));
            materiales.add(crearMaterial("Arena lavada", "Arena fina para mezclas de concreto", "m³"));
            materiales.add(crearMaterial("Grava triturada", "Grava de 1/2\" para concretos", "m³"));
            materiales.add(crearMaterial("Varilla corrugada", "Acero de refuerzo para estructuras", "kg"));
            materiales.add(crearMaterial("Madera para formaleta", "Madera para encofrados", "m²"));
            materiales.add(crearMaterial("Alambre de amarre", "Alambre negro para amarre de varillas", "kg"));
        } else if (descLower.contains("muro") || descLower.contains("pared") || descLower.contains("ladrillo") || descLower.contains("bloque")) {
            materiales.add(crearMaterial("Ladrillo o bloque", "Unidad de mampostería", "und"));
            materiales.add(crearMaterial("Mezcla para pega", "Mezcla adhesiva para mampostería", "kg"));
            materiales.add(crearMaterial("Mortero", "Mezcla para asentado y repellado", "m³"));
            materiales.add(crearMaterial("Varilla de refuerzo", "Acero para muros estructurales", "kg"));
        } else if (descLower.contains("cubierta") || descLower.contains("techo") || descLower.contains("entramado") || descLower.contains("tubo")) {
            materiales.add(crearMaterial("Tubo estructural", "Tubo cuadrado para estructura", "m"));
            materiales.add(crearMaterial("Soldadura", "Material de soldadura para uniones", "kg"));
            materiales.add(crearMaterial("Pintura anticorrosiva", "Pintura para protección de metales", "gl"));
            materiales.add(crearMaterial("Placa de cubierta", "Lámina para cubierta", "m²"));
            materiales.add(crearMaterial("Pernos y tornillos", "Elementos de fijación", "und"));
        } else if (descLower.contains("piso") || descLower.contains("loseta") || descLower.contains("cerámica")) {
            materiales.add(crearMaterial("Losetas o cerámicas", "Acabado para pisos", "m²"));
            materiales.add(crearMaterial("Pegamento para pisos", "Adhesivo para instalación", "kg"));
            materiales.add(crearMaterial("Lechada", "Material para juntas", "kg"));
            materiales.add(crearMaterial("Impermeabilizante", "Protección contra humedad", "gl"));
        } else {
            // Generador por defecto para casos no específicos
            materiales.add(crearMaterial("Material básico de construcción", "Material general para la actividad", "und"));
            materiales.add(crearMaterial("Herramientas manuales", "Equipo básico de trabajo", "und"));
            materiales.add(crearMaterial("Elementos de seguridad", "Equipo de protección personal", "und"));
            materiales.add(crearMaterial("Materiales diversos", "Insumos varios para la actividad", "und"));
        }

        System.out.println("✅ Generados " + materiales.size() + " materiales localmente");
        cacheService.guardarEnCache(hash, materiales);
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