package com.example.servicioWeb;

import com.example.domain.Apu;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentConversation {
    private String userId;
    //Solo guardar el ID del APU, no el objeto completo
    private Long apuId;  // ← Cambiar de Apu apuOriginal (objeto) a Long apuId
    private String apuNombre;  // Opcional: guardar nombre para mostrar


    private List<Map<String, String>> ultimosMateriales;
    private List<String> historialFeedback;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private int interactionCount;

    public AgentConversation(String userId, Apu apu) {
        this.userId = userId;
        this.apuId = apu != null ? apu.getIdAPU() : null;
        this.apuNombre = apu != null ? apu.getNombreAPU() : null;
        this.ultimosMateriales = new ArrayList<>();
        this.historialFeedback = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.interactionCount = 1;
    }
}