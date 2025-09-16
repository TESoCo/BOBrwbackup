package com.example.domain;

import lombok.Data;

import java.io.Serializable;

// Composite ID class for ActividadesObra
@Data
public class ApusObraId implements Serializable {
    private Integer obra;
    private Integer apu;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApusObraId that = (ApusObraId) o;

        if (!obra.equals(that.obra)) return false;
        return apu.equals(that.apu);
    }

    @Override
    public int hashCode() {
        int result = obra.hashCode();
        result = 31 * result + apu.hashCode();
        return result;
    }
}
