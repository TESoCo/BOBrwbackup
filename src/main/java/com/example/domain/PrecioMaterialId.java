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
    private static final long serialVersionUID = 1L;

    @Column(name = "id_material")
    private Long idMaterial;

    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrecioMaterialId that = (PrecioMaterialId) o;
        return Objects.equals(idMaterial, that.idMaterial) &&
                Objects.equals(idProveedor, that.idProveedor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMaterial, idProveedor);
    }
}