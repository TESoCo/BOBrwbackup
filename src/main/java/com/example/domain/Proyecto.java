package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "proyecto",
        indexes = {
                @Index(name = "idx_proyecto_equipo", columnList = "id_equipo"),
                @Index(name = "idx_proyecto_codigo", columnList = "codigo_proyecto")
        })
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Proyecto")
    private Long idProyecto;

    @NotEmpty(message = "La descripción del proyecto es obligatoria")
    @Column(name = "descripcion_proyecto", nullable = false, columnDefinition = "TEXT")
    private String descProyecto;

    @Column(name = "codigo_proyecto", length = 50, unique = true)
    private String codigoProyecto;

    @Column(name = "nombre_proyecto", length = 255, unique = true)
    private String nombreProyecto;


    // ---------- RELACIONES ----------
    //Relacion muchos a uno con equipo, un equipo puede ser asignado a varios proyectos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipo", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Equipo equipo;

    // RELACIÓN CON OBRAS
    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    @JsonManagedReference("proyecto-obras")  // ← PADRE: se serializa
    @ToString.Exclude
    private List<Obra> obras = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ---------- MÉTODOS ----------
    public void agregarObra(Obra obra) {
        obras.add(obra);
        obra.setProyecto(this);
    }

    public void eliminarObra(Obra obra) {
        obras.remove(obra);
        obra.setProyecto(null);
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (codigoProyecto == null) {
            codigoProyecto = "PROY-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}