// Material.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "material")
@SecondaryTables({
        @SecondaryTable(name = "precios_material", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Material")),
        @SecondaryTable(name = "proveedores_material", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Material"))
})
public class Material implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Material")
    private Long idMaterial;

    // From caracteristicas_material
    @Column(name = "unidad_Material", nullable = false)
    private String unidadMaterial;

    @Column(name = "nombre_Material")
    private String nombreMaterial;

    @Column(name = "descripcion_material")
    private String descripcionMaterial;


    // From precios_material
    @Column(name = "precio_Material", table = "precios_material", nullable = false)
    private BigDecimal precioMaterial;


    // From proveedores_material (proveedor ID)
    @ManyToMany(mappedBy = "materialList", fetch = FetchType.EAGER)
    @JsonManagedReference("proveedor-material")
    private List<Proveedor> proveedorList;;


    //RELACIONES INVERSAS PARA OTRAS ENTIDADES

    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL)
    @JsonManagedReference("materialesapu-material")
    private List<MaterialesApu> materialesApus = new ArrayList<>();

    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL)
    @JsonManagedReference("materialesinventario-material")
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();




}