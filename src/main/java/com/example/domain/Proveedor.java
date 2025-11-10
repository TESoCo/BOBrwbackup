package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    private Long idProveedor;


    // One Proveedor has One primary contact Persona
    @ManyToOne
    @JoinColumn(name = "id_Persona", referencedColumnName = "id_Persona")
    private Persona idPersona;


    // Commercial Information for the SUPPLIER (the company itself)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_Info_Comerc", referencedColumnName = "id_Info_Comerc")
    private InformacionComercial informacionComercial;

    // From proveedores_material (proveedor ID)
    @ManyToMany(fetch = FetchType.EAGER)//  Tipo de relación y carga
    @JoinTable(//  Define la tabla intermedia
            name = "proveedores_material", //  Nombre de la tabla junction
            joinColumns = @JoinColumn(name = "id_Proveedor"),//  Columna de esta entidad
            inverseJoinColumns = @JoinColumn(name = "id_Material") //  Columna de la otra entidad
    )
    @JsonBackReference("proveedor-material")
    private List<Material> materialList;




}
