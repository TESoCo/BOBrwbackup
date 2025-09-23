// MaterialesApuId.java
package com.example.domain;
import java.io.Serializable;
import java.util.Objects;

public class MaterialesApuId implements Serializable {
    private Long apu;
    private Long material;

    //Lombok no puede generar los constructores para JPA correctamente por eso toca armarlo asi
    // Constructors, equals, and hashCode methods

    public MaterialesApuId() {}// Constructor por defecto OBLIGATORIO

    public MaterialesApuId(Long apu, Long material) {// Constructor con parámetros
        this.apu = apu;
        this.material = material;
    }
    // EQUALS - Compara ambos campos de la clave
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialesApuId)) return false;
        MaterialesApuId that = (MaterialesApuId) o;
        return apu.equals(that.apu) && material.equals(that.material);
    }

    // HASHCODE - Genera hash basado en ambos campos
    @Override
    public int hashCode() {
        return Objects.hash(apu, material);
    }

    // Getters y Setters
    public Long getApu() { return apu; }
    public void setApu(Long apu) { this.apu = apu; }

    public Long getMaterial() { return material; }
    public void setMaterial(Long material) { this.material = material; }
}



