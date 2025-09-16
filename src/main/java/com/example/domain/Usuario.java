package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "datos_usuario")

@SecondaryTable(name="persona_y_rol_usuario" , pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Usuario"))

public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotEmpty
    @Column(name = "nombre_usuario", table = "datos_usuario", unique = true)
    private String nombreUsuario;

    @NotEmpty
    @Column(name = "apellido_usuario", table = "datos_usuario", unique = true)
    private String apellidoUsuario;

    @NotEmpty
    @Column(name = "contrasena", table = "datos_usuario", unique = true)
    private String pass_usuario;

    @NotEmpty
    @Column(name = "cargo", table = "datos_usuario", unique = true)
    private String cargo;

    @NotEmpty
    @Column(name = "foto_perfil", table = "datos_usuario", unique = true)
    private String fotoPerfil;

    //Relacion muchos a uno con rol
    @ManyToOne
    @JoinColumn(name = "persona_y_rol_usuario", referencedColumnName = "id_rol")
    private Rol rol;

    //Relacion de uno a uno con persona
    @OneToOne
    @JoinColumn(name = "id_Persona", table = "persona_y_rol_usuario", referencedColumnName = "id_Persona", nullable = false)
   private Persona idPersona;

}