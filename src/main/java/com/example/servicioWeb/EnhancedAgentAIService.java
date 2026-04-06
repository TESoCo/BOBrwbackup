package com.example.servicioWeb;

import com.example.domain.Apu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhancedAgentAIService {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AIService aiService;

    public AgentResponse generarMaterialesConContexto(
            String sessionId,
            String userId,
            Apu apu,
            String feedback) {

        System.out.println("=== AGENTE IA GENERANDO MATERIALES ===");
        System.out.println("APU: " + apu.getNombreAPU());

        try {
            AgentConversation conversation = conversationService.getConversation(sessionId);

            if (conversation == null) {
                conversation = new AgentConversation(userId, apu);
                System.out.println("📝 Nueva conversación creada");
            }

            List<Map<String, String>> materiales;

            if (feedback != null && !feedback.isEmpty()) {
                System.out.println("🔄 Refinando con feedback: " + feedback);
                materiales = refinarMateriales(conversation, feedback);
            } else {
                System.out.println("🤖 Generando materiales desde IA...");
                materiales = aiService.generarMateriales(apu.getDescAPU());
            }

            List<String> sugerencias = generarSugerencias(materiales);
            List<String> preguntas = generarPreguntas(materiales);

            conversation.setUltimosMateriales(materiales);
            conversation.setLastUpdated(java.time.LocalDateTime.now());
            conversation.setInteractionCount(conversation.getInteractionCount() + 1);

            if (feedback != null && !feedback.isEmpty()) {
                conversation.getHistorialFeedback().add(feedback);
            }

            conversationService.saveConversation(sessionId, conversation);

            String mensaje = generarMensajeContextual(conversation);

            AgentResponse response = new AgentResponse(materiales, mensaje);
            response.setSugerencias(sugerencias);
            response.setPreguntas(preguntas);

            return response;

        } catch (Exception e) {
            System.err.println("❌ Error en agente IA: " + e.getMessage());
            e.printStackTrace();

            AgentResponse errorResponse = new AgentResponse(
                    new ArrayList<>(),
                    "Lo siento, tuve un problema generando los materiales: " + e.getMessage()
            );
            return errorResponse;
        }
    }

    private List<Map<String, String>> refinarMateriales(
            AgentConversation conversation,
            String feedback) {

        List<Map<String, String>> materialesActuales = conversation.getUltimosMateriales();

        try {
            String promptRefinamiento = construirPromptRefinamiento(materialesActuales, feedback);
            return aiService.generarMateriales(promptRefinamiento);
        } catch (Exception e) {
            return refinarManual(materialesActuales, feedback);
        }
    }

    private List<Map<String, String>> refinarManual(
            List<Map<String, String>> materiales,
            String feedback) {

        List<Map<String, String>> resultado = new ArrayList<>(materiales);
        String feedbackLower = feedback.toLowerCase();

        if (feedbackLower.contains("eliminar") || feedbackLower.contains("quitar")) {
            for (String palabra : feedbackLower.split(" ")) {
                resultado.removeIf(m ->
                        m.get("nombre").toLowerCase().contains(palabra) ||
                                m.get("descripcion").toLowerCase().contains(palabra)
                );
            }
        }

        if (feedbackLower.contains("agregar") || feedbackLower.contains("añadir")) {
            Map<String, String> nuevoMaterial = new HashMap<>();
            nuevoMaterial.put("nombre", "Material adicional");
            nuevoMaterial.put("descripcion", feedback);
            nuevoMaterial.put("unidad", "und");
            resultado.add(nuevoMaterial);
        }

        return resultado;
    }

    private List<String> generarSugerencias(List<Map<String, String>> materiales) {
        List<String> sugerencias = new ArrayList<>();

        boolean tieneConcreto = materiales.stream()
                .anyMatch(m -> m.get("nombre").toLowerCase().contains("concreto") ||
                        m.get("nombre").toLowerCase().contains("cemento"));

        if (tieneConcreto) {
            sugerencias.add("💡 ¿Necesitas incluir aditivos para el concreto?");
            sugerencias.add("💡 Considera agregar vibrador para la compactación");
        }

        if (materiales.size() < 3) {
            sugerencias.add("💡 La lista tiene pocos materiales. ¿Quieres generar más?");
        }

        if (sugerencias.isEmpty()) {
            sugerencias.add("💡 ¿Los materiales generados son adecuados?");
        }

        return sugerencias;
    }

    private List<String> generarPreguntas(List<Map<String, String>> materiales) {
        List<String> preguntas = new ArrayList<>();
        preguntas.add("¿Falta algún material?");
        preguntas.add("¿Las unidades son correctas?");
        if (materiales.size() > 8) {
            preguntas.add("¿Quieres eliminar algún material?");
        }
        return preguntas;
    }

    private String construirPromptRefinamiento(
            List<Map<String, String>> materiales,
            String feedback) {

        StringBuilder sb = new StringBuilder();
        sb.append("Lista actual de materiales:\n");
        for (Map<String, String> m : materiales) {
            sb.append("- ").append(m.get("nombre"))
                    .append(" (").append(m.get("unidad")).append("): ")
                    .append(m.get("descripcion")).append("\n");
        }
        sb.append("\nFeedback del usuario: ").append(feedback);
        sb.append("\n\nAjusta la lista según el feedback. Responde SOLO con un array JSON.");

        return sb.toString();
    }

    private String generarMensajeContextual(AgentConversation conversation) {
        int count = conversation.getInteractionCount();
        if (count == 1) {
            return "✅ He generado los materiales. ¿Qué te parece?";
        } else {
            return "✅ He ajustado la lista según tu feedback. ¿Algo más?";
        }
    }
}