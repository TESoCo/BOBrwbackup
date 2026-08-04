package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "precios_material",
        indexes = {
                @Index(name = "idx_precio_material", columnList = "id_material"),
                @Index(name = "idx_precio_proveedor", columnList = "id_proveedor"),
                @Index(name = "idx_precio_vigencia", columnList = "fecha_vigencia_desde, fecha_vigencia_hasta"),
                @Index(name = "idx_precio_activo", columnList = "activo")
        })
public class PrecioMaterial implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private PrecioMaterialId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idMaterial")
    @JoinColumn(name = "id_material", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idProveedor")
    @JoinColumn(name = "id_proveedor", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Proveedor proveedor;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @NotNull(message = "La fecha de vigencia es obligatoria")
    @Column(name = "fecha_vigencia_desde", nullable = false)
    private LocalDateTime fechaVigenciaDesde;

    @Column(name = "fecha_vigencia_hasta")
    private LocalDateTime fechaVigenciaHasta;

    @Column(name = "activo")
    private Boolean activo = true;

    @DecimalMin(value = "0.0", message = "El precio mínimo de compra no puede ser negativo")
    @Column(name = "precio_minimo_compra", precision = 15, scale = 2)
    private BigDecimal precioMinimoCompra;

    @DecimalMin(value = "0.0", message = "El descuento por volumen no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede ser mayor a 100%")
    @Column(name = "descuento_volumen", precision = 5, scale = 2)
    private BigDecimal descuentoVolumen;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;


    // ---------- AUDITORÍA ----------
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- CONSTRUCTORES ----------
    public PrecioMaterial() {}

    public PrecioMaterial(Material material, Proveedor proveedor, BigDecimal precioUnitario) {
        this.material = material;
        this.proveedor = proveedor;
        this.precioUnitario = precioUnitario;
        this.id = new PrecioMaterialId(material.getIdMaterial(), proveedor.getIdProveedor());
        this.fechaVigenciaDesde = LocalDateTime.now();
    }


    // ---------- MÉTODOS ----------
    public boolean estaVigente() {
        LocalDateTime now = LocalDateTime.now();
        return activo &&
                fechaVigenciaDesde.isBefore(now) &&
                (fechaVigenciaHasta == null || fechaVigenciaHasta.isAfter(now));
    }

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