// InformacionComercial.java
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
@Table(name = "informacion_comercial",
        indexes = {
                @Index(name = "idx_info_nit", columnList = "nit_rut"),
                @Index(name = "idx_info_email", columnList = "correo_electronico")
        })
public class InformacionComercial implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Info_Comerc")
    private Long idInfoComerc;

    @NotEmpty(message = "El NIT/RUT es obligatorio")
    @Column(name = "nit_rut", nullable = false, unique = true, length = 50)
    private String nitRut;

    @NotEmpty(message = "La forma de pago es obligatoria")
    @Column(name = "forma_pago", nullable = false, length = 100)
    private String formaPago;

    @NotEmpty(message = "El banco es obligatorio")
    @Column(name = "banco", nullable = false, length = 100)
    private String banco;

    @NotEmpty(message = "El número de cuenta es obligatorio")
    @Column(name = "numero_cuenta", nullable = false, length = 50)
    private String numCuenta;

    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ\\d\\s,.#-]+$",
            message = "La dirección contiene caracteres no permitidos. Use solo letras, números, espacios, comas, puntos, # y guiones"
    )
    @NotEmpty(message = "La dirección es obligatoria")
    @Column(name = "direccion", nullable = false, columnDefinition = "TEXT")
    private String direccion;

    @Email(message = "El correo electrónico no es válido")
    @NotEmpty(message = "El correo electrónico es obligatorio")
    @Column(name = "correo_electronico", nullable = false, length = 255)
    private String correoElectronico;

    @NotEmpty(message = "El producto/servicio es obligatorio")
    @Column(name = "producto", nullable = false, length = 255)
    private String producto;


    // ---------- RELACIONES ----------
    @OneToMany(mappedBy = "informacionComercial", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Proveedor> proveedores = new ArrayList<>();

    @OneToMany(mappedBy = "informacionComercial", fetch = FetchType.LAZY)
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