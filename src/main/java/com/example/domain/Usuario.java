package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "usuario",
        indexes = {
                @Index(name = "idx_usuario_nombre", columnList = "nombre_usuario"),
                @Index(name = "idx_usuario_email", columnList = "email"),
                @Index(name = "idx_usuario_rol", columnList = "id_rol"),
                @Index(name = "idx_usuario_equipo", columnList = "id_equipo"),
                @Index(name = "idx_usuario_status", columnList = "status")
        })


public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---------- ENUMS ----------
    public enum StatusUsuario {
        PENDING, APPROVED, REJECTED, SUSPENDED
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, MICROSOFT
    }

    //Tabla "datos_usuario"
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Usuario")
    private Long idUsuario;


    // ---------- DATOS DE USUARIO ----------
    @NotEmpty(message = "El nombre de usuario es obligatorio")
    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 100)
    private String nombreUsuario;

    @Email(message = "Correo electrónico inválido")
    @NotEmpty(message = "El correo electrónico es obligatorio")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Column(name = "contrasena", nullable = false)
    @JsonIgnore
    private String pass_usuario;

    @NotEmpty(message = "El cargo es obligatorio")
    @Column(name = "cargo", nullable = false, length = 100)
    private String cargo;


    // ---------- FOTO DE PERFIL ----------
    @Lob // Anotación importante para BLOB
    @Column(name = "foto_Perfil", columnDefinition = "LONGBLOB")
    private byte[] fotoPerfil;

    @Column(name = "foto_tipo", length = 100)
    private String fotoTipo;


    // ---------- AUTENTICACIÓN ----------
    //NUEVAS COLUMNAS PARA REGISTRO DE USUARIOS CON GOOGLE
    //Esta columna controla si el usuario ya ha sido autorizado para ingresar al sistema por un admin
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusUsuario status = StatusUsuario.PENDING; // Valores: PENDING, APPROVED, REJECTED

    //Esta columna nos dice si el usuario fue creado con una cuenta de Google o si fue con el sistema manual
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL; // Valores: LOCAL, GOOGLE

    //Esta columna es para guardar los tokens de seguridad que genera google para actuar en nombre del usuario (como en el sistema de envío de correos)
    @Column(name = "google_refresh_token", columnDefinition = "TEXT")
    @JsonIgnore
    private String googleRefreshToken; // Aquí guardaremos el token encriptado

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;


    // ---------- RELACIONES ----------
    // RELACIÓN CON rol: ManyToOne (un usuario tiene un rol)
    //Relacion muchos a uno con rol, un rol puede ser asignado a varios usuarios
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Rol rol;

    //Relacion  con persona
    // RELACIÓN CON PERSONA: ManyToOne (un usuario tiene una persona, pero una persona puede no ser usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Persona persona;

    // RELACIÓN CON equipo: ManyToOne (un usuario tiene un equipo)
    //Relacion muchos a uno con equipo, un equipo puede ser asignado a varios usuarios
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipo")
    @JsonIgnore
    @ToString.Exclude
    private Equipo equipo;


    // ---------- RELACIONES INVERSAS ----------
    @OneToMany(mappedBy = "idUsuario", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Obra> obrasCreadas = new ArrayList<>();

    @OneToMany(mappedBy = "idUsuario", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Apu> apusCreados = new ArrayList<>();

    @OneToMany(mappedBy = "idUsuario", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Avance> avances = new ArrayList<>();

    @OneToMany(mappedBy = "idUsuario", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Inventario> inventariosSolicitados = new ArrayList<>();

    @OneToMany(mappedBy = "usuarioAprobador", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Inventario> inventariosAprobados = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS DE AYUDA ----------
    public String getNombreCompleto() {
        if (persona != null) {
            return persona.getNombre() + " " + persona.getApellido();
        }
        return nombreUsuario;
    }

    public boolean tienePermiso(String nombrePermiso) {
        if (rol == null || rol.getPermisoList() == null) {
            return false;
        }
        return rol.getPermisoList().stream()
                .anyMatch(p -> p.getNombrePermiso().equals(nombrePermiso));
    }

    public boolean isApproved() {
        return status == StatusUsuario.APPROVED;
    }


    // ---------- HOOKS JPA ----------
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (email == null && persona != null) {
            email = persona.getCorreo();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }


}