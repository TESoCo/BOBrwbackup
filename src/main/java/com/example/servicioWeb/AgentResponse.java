package com.example.servicioWeb;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AgentResponse {
    private List<Map<String, String>> materiales;
    private String mensaje;
    private List<String> sugerencias;
    private List<String> preguntas;

    public AgentResponse(List<Map<String, String>> materiales, String mensaje) {
        this.materiales = materiales;
        this.mensaje = mensaje;
    }
}