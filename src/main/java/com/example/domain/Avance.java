package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "avance",
        indexes = {
                @Index(name = "idx_avance_obra", columnList = "id_obra"),
                @Index(name = "idx_avance_apu", columnList = "id_apu"),
                @Index(name = "idx_avance_contratista", columnList = "id_contratista"),
                @Index(name = "idx_avance_fecha", columnList = "fecha_avance"),
                @Index(name = "idx_avance_usuario", columnList = "id_usuario")
        })

@SecondaryTables({
        @SecondaryTable(name = "fecha_avance", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance")),
        @SecondaryTable(name = "cantidad_avance", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance"))
})

public class Avance implements Serializable{
    private static final long serialVersionUID = 1L;



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avance")
    private Long idAvance;


    // ---------- RELACIONES ----------

    // From CONTEXTO_AVANCE
    @ManyToOne(fetch = FetchType.LAZY)//una obra puede crear muchos avances
    @JoinColumn(name = "id_Obra", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Obra idObra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Contratista")
    @JsonIgnore
    @ToString.Exclude
    private Contratista idContratista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Usuario idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_apu", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Apu idApu;


    // ---------- DATOS DEL AVANCE ----------
    // From FECHA_AVANCE
    @NotNull(message = "La fecha de avance es obligatoria")
    @Column(name = "fecha_avance", nullable = false)
    private LocalDate fechaAvance;

    // From CANTIDAD_AVANCE
    @NotNull(message = "La cantidad ejecutada es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad ejecutada no puede ser negativa")
    @Column(name = "cantidad_ejecutada", nullable = false)
    private Double cantEjec;

    @Column(name = "porcentaje_avance")
    private Double porcentajeAvance;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "anulado")
    private Boolean anulado = false;

    // ---------- AUDITORÍA ----------
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ---------- MÉTODOS ----------
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        calcularPorcentajeAvance();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        calcularPorcentajeAvance();
    }

    public void calcularPorcentajeAvance() {
        if (idApu != null && idApu.getDuracionAPU() != null &&
                idApu.getDuracionAPU().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal duracion = idApu.getDuracionAPU();
            BigDecimal ejecutado = BigDecimal.valueOf(cantEjec);
            this.porcentajeAvance = ejecutado.divide(duracion, 4,
                            java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
    }



    @Column(name = "anular")
    private boolean anular;







}