package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "preciario",
        indexes = {
                @Index(name = "idx_preciario_nombre", columnList = "nombre_preciario"),
                @Index(name = "idx_preciario_usuario", columnList = "id_usuario_creador")
        })
public class Preciario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Preciario")
    private Long idPreciario;

    @NotEmpty(message = "El nombre del preciario es obligatorio")
    @Column(name = "nombre_preciario", nullable = false, length = 255)
    private String nombrePreciario;

    @Column(name = "descripcion_preciario", columnDefinition = "TEXT")
    private String descripcionPreciario;

    @Column(name = "codigo_preciario", length = 50, unique = true)
    private String codigoPreciario;

    // ---------- RELACIONES ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Usuario idUsuario;

    @OneToMany(mappedBy = "preciario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("preciario-apus")
    @ToString.Exclude
    private List<PreciarioApu> apusList = new ArrayList<>();

    @OneToMany(mappedBy = "preciario", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Proyecto> proyectos = new ArrayList<>();

    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ---------- MÉTODOS DE AYUDA ----------
    public void agregarApu(Apu apu, Integer orden) {
        PreciarioApu pa = new PreciarioApu(this, apu, orden);
        this.apusList.add(pa);
    }

    public void eliminarApu(Apu apu) {
        this.apusList.removeIf(pa -> pa.getApu().equals(apu));
    }

    public List<Apu> getApus() {
        return apusList.stream()
                .map(PreciarioApu::getApu)
                .toList();
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (codigoPreciario == null) {
            codigoPreciario = "PREC-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}