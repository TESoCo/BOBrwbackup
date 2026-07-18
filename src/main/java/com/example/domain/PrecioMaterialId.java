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
public class PrecioMaterialId implements Serializable {

    @Column(name = "id_material")
    private Long material;

    @Column(name = "id_proveedor")
    private Long proveedor;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrecioMaterialId that = (PrecioMaterialId) o;
        return Objects.equals(material, that.material) &&
                Objects.equals(proveedor, that.proveedor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, proveedor);
    }
}