package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "precios_material")
@IdClass(PrecioMaterialId.class)
public class PrecioMaterial implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Material", nullable = false)
    private Material material;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "fecha_vigencia_desde", nullable = false)
    private LocalDateTime fechaVigenciaDesde;

    @Column(name = "fecha_vigencia_hasta")
    private LocalDateTime fechaVigenciaHasta;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "precio_minimo_compra")
    private BigDecimal precioMinimoCompra;

    @Column(name = "descuento_volumen")
    private BigDecimal descuentoVolumen;
}