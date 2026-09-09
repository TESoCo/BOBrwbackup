package com.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObraImportDTO {
    private String nombreObra;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Double cooNObra;
    private Double cooEObra;
    private Long idProyecto;
    private List<ApuImportDTO> actividades;
}

