// MaterialesApuId.java
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
public class MaterialesApuId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_apu")
    private Long apu;

    @Column(name = "id_material")
    private Long material;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialesApuId that = (MaterialesApuId) o;
        return Objects.equals(apu, that.apu) &&
                Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apu, material);
    }
}



