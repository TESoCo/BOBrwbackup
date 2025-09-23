package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "usuario")


public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    //Tabla "datos_usuario"
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Usuario")
    private Long idUsuario;

    @NotEmpty
    @Column(name = "nombre_Usuario", unique = true)
    private String nombreUsuario;

    @NotEmpty
    @Column(name = "contrasena", unique = true)
    private String pass_usuario;

    @NotEmpty
    @Column(name = "cargo", unique = true)
    private String cargo;

    @Column(name = "foto_Perfil", unique = true)
    private String fotoPerfil;

    // RELACIÓN CON rol: ManyToOne (un usuario tiene un rol)
    //Relacion muchos a uno con rol, un rol puede ser asignado a varios usuarios
    @ManyToOne
    @JoinColumn(name = "id_Rol", referencedColumnName = "id_Rol")
    private Rol rol;

    //Relacion de uno a uno con persona
    // RELACIÓN CON PERSONA: ManyToOne (un usuario tiene una persona, pero una persona puede no ser usuario)
    @ManyToOne
    @JoinColumn(name = "id_Persona", referencedColumnName = "id_Persona", nullable = false)
    private Persona persona;

}