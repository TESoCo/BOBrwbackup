// Contratista.java
package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

@Data
@Entity
@Table(name = "contratista")

public class Contratista implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Contratista")
    private Long idContratista;

    @Column(name = "nombre_Contratista")
    private String nombreContratista;


    // One contractor has One primary contact Persona
    @ManyToOne
    @JoinColumn(name = "id_Persona", referencedColumnName = "id_Persona")
    private Persona idPersona;


    // Commercial Information for the SUPPLIER (the company itself)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_Info_Comerc", referencedColumnName = "id_Info_Comerc")
    private InformacionComercial informacionComercial;
    //informacion_comercial
    //private Long idInfoComerc;
    //private String nitRut;
    //private String formaPago;
    //private String banco;
    //private String numCuenta;
    //private String direccion;
}