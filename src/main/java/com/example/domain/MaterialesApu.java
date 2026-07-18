package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "materiales_apu",
        indexes = {
                @Index(name = "idx_matapu_apu", columnList = "id_apu"),
                @Index(name = "idx_matapu_material", columnList = "id_material")
        })


public class MaterialesApu implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private MaterialesApuId id;

    // Relación Many-to-One con APU
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("apu")
    @JoinColumn(name = "id_APU", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Apu apu;

    // Relación Many-to-One con material
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("material")
    @JoinColumn(name = "id_material", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Material material;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Column(name = "cantidad", nullable = false)
    private Double cantidad;

    @Column(name = "costo_unitario", precision = 15, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    // ---------- CONSTRUCTORES ----------
    public MaterialesApu() {}

    public MaterialesApu(Apu apu, Material material, Double cantidad) {
        this.apu = apu;
        this.material = material;
        this.cantidad = cantidad;
        this.id = new MaterialesApuId(apu.getIdAPU(), material.getIdMaterial());
        recalcularSubtotal();
    }

    // ---------- MÉTODOS ----------
    public void recalcularSubtotal() {
        if (material != null && cantidad != null) {
            this.costoUnitario = material.getPrecioActual();
            this.subtotal = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        recalcularSubtotal();
    }

}
