package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table (name = "proveedor")

public class Proveedor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Proveedor")
    private Integer idProveedor;


    // One Proveedor has One primary contact Persona
    @OneToOne
    @JoinColumn(name = "id_Persona", referencedColumnName = "id_Persona")
    private Persona idPersona;



    // Commercial Information for the SUPPLIER (the company itself)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_Info_Comerc", referencedColumnName = "id_Info_Comerc")
    private InformacionComercial informacionComercial;

}
