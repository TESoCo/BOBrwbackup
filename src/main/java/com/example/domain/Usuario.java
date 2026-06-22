package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.ToString;

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
    @Column(name = "cargo")
    private String cargo;

    @Lob // Anotación importante para BLOB
    @Column(name = "foto_Perfil", columnDefinition = "LONGBLOB")
    private byte[] fotoPerfil;

    @Column(name = "foto_tipo")
    private String fotoTipo;

    // RELACIÓN CON rol: ManyToOne (un usuario tiene un rol)
    //Relacion muchos a uno con rol, un rol puede ser asignado a varios usuarios
    @ManyToOne
    @JoinColumn(name = "id_Rol", referencedColumnName = "id_Rol")
    @JsonBackReference("usuario-rol")
    @ToString.Exclude
    private Rol rol;

    //Relacion  con persona
    // RELACIÓN CON PERSONA: ManyToOne (un usuario tiene una persona, pero una persona puede no ser usuario)
    @ManyToOne
    @JoinColumn(name = "id_Persona", referencedColumnName = "id_Persona", nullable = false)
    private Persona persona;

    // RELACIÓN CON equipo: ManyToOne (un usuario tiene un equipo)
    //Relacion muchos a uno con equipo, un equipo puede ser asignado a varios usuarios
    @ManyToOne
    @JoinColumn(name = "id_Equipo", referencedColumnName = "id_Equipo")
    @JsonBackReference("usuario-equipo")
    private Equipo equipo;

    //NUEVAS COLUMNAS PARA REGISTRO DE USUARIOS CON GOOGLE

    //Esta columna controla si el usuario ya ha sido autorizado para ingresar al sistema por un admin
    @Column(name = "status")
    private String status = "PENDING"; // Valores: PENDING, APPROVED, REJECTED

    //Esta columna nos dice si el usuario fue creado con una cuenta de Google o si fue con el sistema manual
    @Column(name = "auth_provider")
    private String authProvider; // Valores: LOCAL, GOOGLE

    //Esta columna es para guardar los tokens de seguridad que genera google para actuar en nombre del usuario (como en el sistema de envío de correos)
    @Column(name = "google_refresh_token", columnDefinition = "TEXT")
    private String googleRefreshToken; // Aquí guardaremos el token encriptado

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

}