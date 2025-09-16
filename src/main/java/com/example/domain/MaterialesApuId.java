// MaterialesApuId.java
package com.example.domain;
import java.io.Serializable;

public class MaterialesApuId implements Serializable {
    private Integer apu;
    private Integer material;

    // Constructors, equals, and hashCode methods
    public MaterialesApuId() {}

    public MaterialesApuId(Integer apu, Integer material) {
        this.apu = apu;
        this.material = material;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialesApuId)) return false;
        MaterialesApuId that = (MaterialesApuId) o;
        return apu.equals(that.apu) && material.equals(that.material);
    }

    @Override
    public int hashCode() {
        int result = apu.hashCode();
        result = 31 * result + material.hashCode();
        return result;
    }
}