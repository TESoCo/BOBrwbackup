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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rol",
        indexes = {
                @Index(name = "idx_rol_nombre", columnList = "nombre_rol")
        })
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    //tabla "rol"
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Rol")
    private Long idRol;

    @NotEmpty(message = "El nombre del rol es obligatorio")
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 100)
    private String nombreRol;

    @NotEmpty(message = "La descripción del rol es obligatoria")
    @Column(name = "descripcion_rol", nullable = false, length = 255)
    private String descripRol;


    // ---------- RELACIONES ----------
    //Tabla "rol_permiso"
    // LADO PROPIETARIO - mantener JoinTable
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Permiso> permisoList;

    // Relación inversa con Usuario
    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    @JsonManagedReference("usuario-rol")
    @ToString.Exclude //Esto debe curar el bucle de cargas
    private List<Usuario> usuarios;


    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS ----------
    public void agregarPermiso(Permiso permiso) {
        if (!permisoList.contains(permiso)) {
            permisoList.add(permiso);
        }
    }

    public void eliminarPermiso(Permiso permiso) {
        permisoList.remove(permiso);
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

}
