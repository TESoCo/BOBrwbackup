package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
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
@Table(name = "persona",
        indexes = {
                @Index(name = "idx_persona_correo", columnList = "correo"),
                @Index(name = "idx_persona_identificacion", columnList = "identificacion")
        })
public class Persona implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Persona")
    private Long idPersona;

    @NotEmpty(message = "El nombre no puede estar vacío")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotEmpty(message = "El apellido no puede estar vacío")
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "identificacion", length = 50, unique = true)
    private String identificacion;

    @Pattern(regexp = "^[0-9+\\-() ]+$", message = "Teléfono inválido")
    @NotEmpty(message = "El teléfono no puede estar vacío")
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Email(message = "Correo electrónico inválido")
    @NotEmpty(message = "El correo no puede estar vacío")
    @Column(name = "correo", nullable = false, unique = true, length = 255)
    private String correo;


    @Column(name = "direccion", columnDefinition = "TEXT")
    private String direccion;

    // ---------- RELACIONES ----------
    @OneToMany(mappedBy = "persona", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany(mappedBy = "idPersona", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Proveedor> proveedores = new ArrayList<>();

    @OneToMany(mappedBy = "idPersona", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Contratista> contratistas = new ArrayList<>();

    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ---------- MÉTODOS DE AYUDA ----------
    public String getNombreCompleto() {
        return nombre + " " + apellido;
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

