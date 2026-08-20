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
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Equipo")
    private Long idEquipo;

    @NotEmpty(message = "La descripción del equipo es obligatoria")
    @Column(name = "descripcion_equipo", nullable = false, length = 255, unique = true)
    private String descEquipo;

    @Column(name = "codigo_equipo", length = 50, unique = true)
    private String codigoEquipo;


    // ---------- RELACIONES ----------
    // Agregar relación inversa con Usuario
    @OneToMany(mappedBy = "equipo", fetch = FetchType.LAZY)
    @JsonManagedReference("usuario-equipo")// ← PADRE: se serializa
    @ToString.Exclude
    private List<Usuario> usuarios = new ArrayList<>();

    // Agregar relación inversa con proyecto
    @OneToMany(mappedBy = "equipo", fetch = FetchType.LAZY)
    @JsonManagedReference("equipo-proyectos")
    @ToString.Exclude
    private List<Proyecto> proyectos = new ArrayList<>();

    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (codigoEquipo == null) {
            codigoEquipo = "EQ-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // ---------- MÉTODOS DE AYUDA ----------
    public void agregarUsuario(Usuario usuario) {
        usuarios.add(usuario);
        usuario.setEquipo(this);
    }

    public void eliminarUsuario(Usuario usuario) {
        usuarios.remove(usuario);
        usuario.setEquipo(null);
    }

}