package com.example.domain;

import lombok.Data;
import java.io.Serializable;
import jakarta.persistence.*;

@Data
@Embeddable
public class ApusObraId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_Obra")
    private Obra obra;

    @ManyToOne
    @JoinColumn(name = "id_APU")
    private Apu apu;

    // Constructors
    public ApusObraId() {}

    public ApusObraId(Obra idObra, Apu idAPU) {
        this.obra = idObra;
        this.apu = idAPU;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApusObraId that = (ApusObraId) o;
        return obra.equals(that.obra) && apu.equals(that.apu);
    }

    @Override
    public int hashCode() {
        int result = obra.hashCode();
        result = 31 * result + apu.hashCode();
        return result;
    }
}