package com.example.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "material")
/*
@SecondaryTables({
        @SecondaryTable(name = "precios_material", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Material"))
})
*/
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

    // Relación con precios por proveedor (1:N)
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("material-precios")
    private List<PrecioMaterial> preciosPorProveedor = new ArrayList<>();

    // From proveedores_material (proveedor ID) Relación ManyToMany con proveedores
    @ManyToMany(mappedBy = "materialList", fetch = FetchType.EAGER)
    @JsonManagedReference("proveedor-material")
    private List<Proveedor> proveedorList = new ArrayList<>();


    //RELACIONES INVERSAS PARA OTRAS ENTIDADES

    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL)
    @JsonManagedReference("materialesapu-material")
    private List<MaterialesApu> materialesApus = new ArrayList<>();

    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL)
    @JsonManagedReference("materialesinventario-material")
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();

    // helper: Obtener precio actual del mejor proveedor
    public BigDecimal getPrecioActual() {
        return preciosPorProveedor.stream()
                .filter(p -> p.getActivo() && p.getFechaVigenciaHasta() == null)
                .map(PrecioMaterial::getPrecioUnitario)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    // helper: Obtener precio de un proveedor específico
    public BigDecimal getPrecioPorProveedor(Long proveedorId) {
        return preciosPorProveedor.stream()
                .filter(p -> p.getProveedor().getIdProveedor().equals(proveedorId))
                .filter(p -> p.getActivo() && p.getFechaVigenciaHasta() == null)
                .findFirst()
                .map(PrecioMaterial::getPrecioUnitario)
                .orElse(null);
    }

    @Column(name = "stock_disponible", columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private Double stockDisponible = 0.0;

    @Column(name = "stock_reservado", columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private Double stockReservado = 0.0;

    @Column(name = "stock_minimo", columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private Double stockMinimo = 0.0;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "activo")
    private Boolean activo = true;

    // Métod para obtener stock total
    public Double getStockTotal() {
        return stockDisponible + stockReservado;
    }

    // Métod para verificar si hay stock disponible
    public boolean hayStockDisponible(Double cantidad) {
        return stockDisponible >= cantidad;
    }


}