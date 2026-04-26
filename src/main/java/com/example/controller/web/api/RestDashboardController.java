package com.example.controller.web.api;

import com.example.domain.Avance;
import com.example.domain.Obra;
import com.example.servicio.AvanceServicio;
import com.example.servicio.ObraServicio;
import com.example.servicio.InventarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class RestDashboardController {

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private InventarioServicio inventarioServicio;

    /**
     * Resumen general del dashboard
     * GET /api/dashboard/resumen?userId=1 (opcional)
     */
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> getResumen(
            @RequestParam(required = false) Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            long totalObras = obraServicio.listaObra().size();
            long totalAvances = avanceServicio.listaAvance().size();
            long totalInventarios = inventarioServicio.listaInventarios().size();

            response.put("success", true);
            response.put("totalObras", totalObras);
            response.put("totalAvances", totalAvances);
            response.put("totalInventarios", totalInventarios);

            // Si se especifica usuario, agregar avances del usuario
            if (userId != null) {
                long misAvances = avanceServicio.buscarPorIdUsuario(userId).size();
                response.put("misAvances", misAvances);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Avances agrupados por obra (para gráficos)
     * GET /api/dashboard/avances-por-obra
     */
    @GetMapping("/avances-por-obra")
    public ResponseEntity<Map<String, Object>> getAvancesPorObra() {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Obra> obras = obraServicio.listaObra();
            Map<String, Long> avancesPorObra = new HashMap<>();

            for (Obra obra : obras) {
                long count = avanceServicio.buscarPorIdObra(obra.getIdObra()).size();
                avancesPorObra.put(obra.getNombreObra(), count);
            }

            response.put("success", true);
            response.put("data", avancesPorObra);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Últimos avances registrados
     * GET /api/dashboard/ultimos-avances?limit=5
     */
    @GetMapping("/ultimos-avances")
    public ResponseEntity<Map<String, Object>> getUltimosAvances(
            @RequestParam(defaultValue = "5") int limit) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Avance> todosAvances = avanceServicio.listaAvance();

            // Ordenar por fecha descendente y limitar
            List<Avance> ultimosAvances = todosAvances.stream()
                    .sorted((a1, a2) -> a2.getFechaAvance().compareTo(a1.getFechaAvance()))
                    .limit(limit)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("avances", ultimosAvances);
            response.put("count", ultimosAvances.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Resumen de avances por mes (para gráfico de barras)
     * GET /api/dashboard/avances-por-mes?year=2024
     */
    @GetMapping("/avances-por-mes")
    public ResponseEntity<Map<String, Object>> getAvancesPorMes(
            @RequestParam(defaultValue = "2026") int year) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Avance> todosAvances = avanceServicio.listaAvance();

            Map<Integer, Long> avancesPorMes = new HashMap<>();

            // Inicializar meses
            for (int mes = 1; mes <= 12; mes++) {
                avancesPorMes.put(mes, 0L);
            }

            // Contar avances por mes
            for (Avance avance : todosAvances) {
                if (avance.getFechaAvance().getYear() == year) {
                    int mes = avance.getFechaAvance().getMonthValue();
                    avancesPorMes.put(mes, avancesPorMes.get(mes) + 1);
                }
            }

            response.put("success", true);
            response.put("year", year);
            response.put("data", avancesPorMes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Estadísticas rápidas (versión simplificada para Android)
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuickStats() {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Obra> obras = obraServicio.listaObra();
            List<Avance> avances = avanceServicio.listaAvance();

            // Calcular obra más activa
            Map<Long, Long> avancesPorObraId = new HashMap<>();
            for (Avance avance : avances) {
                Long obraId = avance.getIdObra().getIdObra();
                avancesPorObraId.put(obraId, avancesPorObraId.getOrDefault(obraId, 0L) + 1);
            }

            Long obraMasActivaId = null;
            long maxAvances = 0;
            for (Map.Entry<Long, Long> entry : avancesPorObraId.entrySet()) {
                if (entry.getValue() > maxAvances) {
                    maxAvances = entry.getValue();
                    obraMasActivaId = entry.getKey();
                }
            }

            String nombreObraMasActiva = "";
            if (obraMasActivaId != null) {
                for (Obra obra : obras) {
                    if (obra.getIdObra().equals(obraMasActivaId)) {
                        nombreObraMasActiva = obra.getNombreObra();
                        break;
                    }
                }
            }

            response.put("success", true);
            response.put("totalObras", obras.size());
            response.put("totalAvances", avances.size());
            response.put("obraMasActiva", nombreObraMasActiva);
            response.put("promedioAvancesPorObra", obras.isEmpty() ? 0 : (double) avances.size() / obras.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}