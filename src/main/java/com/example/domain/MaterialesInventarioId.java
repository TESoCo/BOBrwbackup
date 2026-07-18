// MaterialesInventarioId.java
package com.example.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable

public class MaterialesInventarioId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_inventario")
    private Long inventario;

    @Column(name = "id_material")
    private Long material;

    // equals, and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialesInventarioId that = (MaterialesInventarioId) o;
        return Objects.equals(inventario, that.inventario) &&
                Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inventario, material);
    }


}