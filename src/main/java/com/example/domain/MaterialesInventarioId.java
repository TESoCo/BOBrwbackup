// MaterialesInventarioId.java
package com.example.domain;
import java.io.Serializable;

public class MaterialesInventarioId implements Serializable {
    private Long idInventario;
    private Integer material;

    // Constructors, equals, and hashCode methods
    public MaterialesInventarioId() {}

    public MaterialesInventarioId(Long inventario, Integer material) {
        this.idInventario = inventario;
        this.material = material;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialesInventarioId)) return false;
        MaterialesInventarioId that = (MaterialesInventarioId) o;
        return idInventario.equals(that.idInventario) && material.equals(that.material);
    }

    @Override
    public int hashCode() {
        int result = idInventario.hashCode();
        result = 31 * result + material.hashCode();
        return result;
    }
}