package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "material",
        indexes = {
                @Index(name = "idx_material_nombre", columnList = "nombre_material"),
                @Index(name = "idx_material_unidad", columnList = "unidad_material"),
                @Index(name = "idx_material_activo", columnList = "activo")
        })
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Material")
    private Long idMaterial;


    // ---------- DATOS DEL MATERIAL ----------
    // From caracteristicas_material
    @NotEmpty(message = "La unidad del material es obligatoria")
    @Column(name = "unidad_material", nullable = false, length = 50)
    private String unidadMaterial;

    @NotEmpty(message = "El nombre del material es obligatorio")
    @Column(name = "nombre_material", nullable = false, length = 255)
    private String nombreMaterial;

    @Column(name = "descripcion_material", columnDefinition = "TEXT")
    private String descripcionMaterial;

    @Column(name = "codigo_material", length = 50, unique = true)
    private String codigoMaterial;


    // ---------- STOCK ----------
    @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
    @Column(name = "stock_disponible")
    private Double stockDisponible = 0.0;

    @DecimalMin(value = "0.0", message = "El stock reservado no puede ser negativo")
    @Column(name = "stock_reservado")
    private Double stockReservado = 0.0;

    @DecimalMin(value = "0.0", message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo")
    private Double stockMinimo = 0.0;

    @Column(name = "ubicacion", length = 255)
    private String ubicacion;


    // ---------- RELACIONES ----------
    // Relación con precios por proveedor (1:N)
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("material-precios")
    @ToString.Exclude
    private List<PrecioMaterial> preciosPorProveedor = new ArrayList<>();

    // From proveedores_material (proveedor ID) Relación ManyToMany con proveedores
    @ManyToMany(mappedBy = "materialList", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Proveedor> proveedorList = new ArrayList<>();


    //RELACIONES INVERSAS PARA OTRAS ENTIDADES
    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<MaterialesApu> materialesApus = new ArrayList<>();

    // Material needs the reverse relationship, para agregar material a los apus
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS DE AYUDA ----------
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

    // Métod para obtener stock total
    public Double getStockTotal() {
        return stockDisponible + stockReservado;
    }

    // Métod para verificar si hay stock disponible
    public boolean hayStockDisponible(Double cantidad) {
        return stockDisponible >= cantidad;
    }

    public void reservarStock(Double cantidad) {
        if (hayStockDisponible(cantidad)) {
            this.stockDisponible -= cantidad;
            this.stockReservado += cantidad;
        } else {
            throw new IllegalStateException("Stock insuficiente");
        }
    }

    public void liberarStock(Double cantidad) {
        if (this.stockReservado >= cantidad) {
            this.stockReservado -= cantidad;
            this.stockDisponible += cantidad;
        } else {
            throw new IllegalStateException("Stock reservado insuficiente");
        }
    }

    public void consumirStock(Double cantidad) {
        if (this.stockReservado >= cantidad) {
            this.stockReservado -= cantidad;
        } else {
            throw new IllegalStateException("Stock reservado insuficiente para consumir");
        }
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (codigoMaterial == null) {
            codigoMaterial = "MAT-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }








}