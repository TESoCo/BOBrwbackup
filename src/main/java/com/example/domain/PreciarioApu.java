package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "preciario_apu",
        indexes = {
                @Index(name = "idx_preciarioapu_preciario", columnList = "id_preciario"),
                @Index(name = "idx_preciarioapu_apu", columnList = "id_apu")
        })
public class PreciarioApu implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private PreciarioApuId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPreciario")
    @JoinColumn(name = "id_preciario", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Preciario preciario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idAPU")
    @JoinColumn(name = "id_apu", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Apu apu;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "fecha_asignacion", updatable = false)
    private LocalDateTime fechaAsignacion;

    // ---------- CONSTRUCTORES ----------
    public PreciarioApu() {
        this.id = new PreciarioApuId();
    }

    public PreciarioApu(Preciario preciario, Apu apu, Integer orden) {
        this.preciario = preciario;
        this.apu = apu;
        this.id = new PreciarioApuId(preciario.getIdPreciario(), apu.getIdAPU());
        this.orden = orden;
        this.fechaAsignacion = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
    }
}