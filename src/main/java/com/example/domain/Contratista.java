        package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
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
@Table(name = "contratista",
        indexes = {
                @Index(name = "idx_contratista_persona", columnList = "id_Persona"),
                @Index(name = "idx_contratista_info_comercial", columnList = "id_Info_Comerc")
        })

public class Contratista implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Contratista")
    private Long idContratista;

    @NotEmpty(message = "El nombre del contratista es obligatorio")
    @Column(name = "nombre_contratista", nullable = false, length = 255)
    private String nombreContratista;

    @Column(name = "codigo_contratista", length = 50, unique = true)
    private String codigoContratista;


    // ---------- RELACIONES ----------
    // One contractor has One primary contact Persona
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Persona", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Persona idPersona;

    // Commercial Information for the SUPPLIER (the company itself)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Info_Comerc", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private InformacionComercial informacionComercial;
    //informacion_comercial
    //private Long idInfoComerc;
    //private String nitRut;
    //private String formaPago;
    //private String banco;
    //private String numCuenta;
    //private String direccion;

    @OneToMany(mappedBy = "idContratista", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Avance> avances = new ArrayList<>();


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
        if (codigoContratista == null) {
            codigoContratista = "CTR-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

}