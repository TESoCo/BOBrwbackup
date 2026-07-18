// MaterialesInventario.java
package com.example.domain;

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

@Getter
@Setter
@Entity
@Table(name = "materiales_inventario",
        indexes = {
                @Index(name = "idx_matinv_inventario", columnList = "id_inventario"),
                @Index(name = "idx_matinv_material", columnList = "id_material")
        })

public class MaterialesInventario implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private MaterialesInventarioId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("inventario")
    @JoinColumn(name = "id_inventario", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Inventario inventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("material")
    @JoinColumn(name = "id_material", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Material material;


    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Column(name = "cantidad", nullable = false)
    private double cantidad;

    @Column(name = "precio_unitario", precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    // ---------- CONSTRUCTORES ----------
    public MaterialesInventario() {}

    public MaterialesInventario(Inventario inventario, Material material, Double cantidad) {
        this.inventario = inventario;
        this.material = material;
        this.cantidad = cantidad;
        this.id = new MaterialesInventarioId(inventario.getIdInventario(),
                material.getIdMaterial());
        recalcularSubtotal();
    }

    // ---------- MÉTODOS ----------
    public void recalcularSubtotal() {
        if (material != null && cantidad <= 0) {
            this.precioUnitario = material.getPrecioActual();
            this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        recalcularSubtotal();
    }

}

