package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ApusObraId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "id_obra")
    private Long idObra;

    @Column(name = "id_apu")
    private Long idApu;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApusObraId that = (ApusObraId) o;
        return Objects.equals(idObra, that.idObra) &&
                Objects.equals(idApu, that.idApu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idObra, idApu);
    }
}