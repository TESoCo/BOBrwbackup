package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Separate entity for ACTIVIDADES_OBRA (many-to-many relationship)
@Getter
@Setter
@Entity
@Table(name = "apus_obra",
        indexes = {
                @Index(name = "idx_apusobra_obra", columnList = "id_obra"),
                @Index(name = "idx_apusobra_apu", columnList = "id_apu")
        })

public class ApusObra implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private ApusObraId id;



    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idObra")
    @JoinColumn(name = "id_obra", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idApu")
    @JoinColumn(name = "id_apu", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Apu apu;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Column(name = "cantidad", nullable = false)
    private Double cantidad = 1.0; // Default quantity

    @Column(name = "precio_unitario", precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "fecha_asignacion", updatable = false)
    private LocalDateTime fechaAsignacion;

    // ---------- CONSTRUCTORES ----------
    public ApusObra() {}

    public ApusObra(Obra obra, Apu apu, Double cantidad) {
        this.obra = obra;
        this.apu = apu;
        this.id = new ApusObraId(obra.getIdObra(), apu.getIdAPU());
        this.cantidad = cantidad;
        this.fechaAsignacion = LocalDateTime.now();
        recalcularSubtotal();
    }

    // ---------- MÉTODOS ----------
    public void recalcularSubtotal() {
        if (apu != null && cantidad != null) {
            this.subtotal = apu.getVTotalApu().multiply(BigDecimal.valueOf(cantidad));
        }
    }

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
        recalcularSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        recalcularSubtotal();
    }




}
