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
public class PreciarioApuId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "id_preciario")
    private Long idPreciario;

    @Column(name = "id_apu")
    private Long idAPU;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreciarioApuId that = (PreciarioApuId) o;
        return Objects.equals(idPreciario, that.idPreciario) &&
                Objects.equals(idAPU, that.idAPU);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPreciario, idAPU);
    }
}