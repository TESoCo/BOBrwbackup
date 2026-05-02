package com.example.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class AvanceRequestDTO {
    // Getters y Setters
    private Long idObra;
    private Long idUsuario;
    private Long idApu;
    private LocalDate fechaAvance;
    private Double cantEjec;

    public void setIdObra(Long idObra) { this.idObra = idObra; }

    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public void setIdApu(Long idApu) { this.idApu = idApu; }

    public void setFechaAvance(LocalDate fechaAvance) { this.fechaAvance = fechaAvance; }

    public void setCantEjec(Double cantEjec) { this.cantEjec = cantEjec; }
}