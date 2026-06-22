package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "persona")
public class Persona implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Persona")
    private Long idPersona;

    @NotEmpty(message = "El nombre no puede estar vacío")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @NotEmpty(message = "El apellido no puede estar vacío")
    @Column(name = "apellido", nullable = false)
    private String apellido;

    @NotEmpty(message = "El teléfono no puede estar vacío")
    @Column(name = "telefono", nullable = false)
    private String telefono;

    @NotEmpty(message = "El correo no puede estar vacío")
    @Column(name = "correo", nullable = false, unique = true)
    private String correo;
}
