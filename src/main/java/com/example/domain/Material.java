// Material.java
package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

@Data
@Entity
@Table(name = "CARACTERISTICAS_MATERIAL")
@SecondaryTables({
        @SecondaryTable(name = "PRECIOS_MATERIAL", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Material")),
        @SecondaryTable(name = "PROVEEDORES_MATERIAL", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Material"))
})
public class Material implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Material")
    private Integer idMaterial;

    // From CARACTERISTICAS_MATERIAL
    @Column(name = "Unidad_Material", nullable = false)
    private String unidadMaterial;

    @Column(name = "Specificaciones")
    private String especificaciones;

    @Column(name = "Certificados")
    private String certificados;

    // From PRECIOS_MATERIAL
    @Column(name = "Precio_Material", table = "PRECIOS_MATERIAL", nullable = false)
    private Double precioMaterial;

    // From PROVEEDORES_MATERIAL (proveedor ID)
    @Column(name = "id_Proveedor", table = "PROVEEDORES_MATERIAL", nullable = false)
    private Proveedor idProveedor;


}