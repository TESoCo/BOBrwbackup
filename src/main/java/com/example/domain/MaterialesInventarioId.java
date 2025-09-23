// MaterialesInventarioId.java
package com.example.domain;
import java.io.Serializable;
import java.util.Objects;

public class MaterialesInventarioId implements Serializable {
    private Long inventario;
    private Long material;

    // Constructors, equals, and hashCode methods
    public MaterialesInventarioId() {}

    public MaterialesInventarioId(Long inventario, Long material) {
        this.inventario = inventario;
        this.material = material;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialesInventarioId)) return false;
        MaterialesInventarioId that = (MaterialesInventarioId) o;
        return inventario.equals(that.inventario) && material.equals(that.material);
    }

    @Override
    public int hashCode() {
            return Objects.hash(inventario, material);
    }

    // Getters y Setters
    public Long getInventario() { return inventario; }
    public void setInventario(Long apu) { this.inventario = apu; }

    public Long getMaterial() { return material; }
    public void setMaterial(Long material) { this.material = material; }


}