// CombinedObraEntity.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.ToString;
import org.apache.poi.hpsf.Decimal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "apu",
        indexes = {
                @Index(name = "idx_apu_usuario", columnList = "id_usuario_creador"),
                @Index(name = "idx_apu_nombre", columnList = "nombre_apu")
        })

public class Apu implements Serializable {
    private static final long serialVersionUID = 1L;


    //`apu`
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_APU")
    private Long idAPU;


    // ---------- DATOS PRINCIPALES ----------
    //`caracteristicas_apu`
    @NotEmpty(message = "El nombre del APU es obligatorio")
    @Column(name = "nombre_apu", nullable = false, length = 255)
    private String nombreAPU;

    @NotEmpty(message = "La descripción del APU es obligatoria")
    @Column(name = "descripcion_apu", nullable = false, columnDefinition = "TEXT")
    private String descAPU;

    @NotEmpty(message = "La unidad del APU es obligatoria")
    @Column(name = "unidad_apu", nullable = false, length = 50)
    private String unidadesAPU;

    @DecimalMin(value = "0.0", inclusive = true, message = "La duración debe ser positiva")
    @Column(name = "duracion_apu", precision = 12, scale = 2)
    private BigDecimal duracionAPU = BigDecimal.ZERO;


    // ---------- VALORES ----------
    //`valor_apu`
    @Column(name = "v_materiales_apu", precision = 15, scale = 2)
    private BigDecimal vMaterialesAPU = BigDecimal.ZERO;

    @Column(name = "v_mano_obra_apu", precision = 15, scale = 2)
    private BigDecimal vManoDeObraAPU = BigDecimal.ZERO;

    @Column(name = "v_transporte_apu", precision = 15, scale = 2)
    private BigDecimal vTransporteAPU = BigDecimal.ZERO;

    @Column(name = "v_misc_apu", precision = 15, scale = 2)
    private BigDecimal vMiscAPU = BigDecimal.ZERO;


    // ---------- VALOR TOTAL CALCULADO ----------
    @Column(name = "v_total_apu", precision = 15, scale = 2)
    private BigDecimal vTotalApu = BigDecimal.ZERO;


    // ---------- RELACIONES ----------
    @ManyToOne(fetch = FetchType.LAZY) //un usuario puede crear muchos APUs
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Usuario idUsuario;

    //apus_obra
    //Relacion muchos a muchos con obra
    // Relación inversa
    // Apu needs the reverse relationship, para agregar apus a las obras
    @OneToMany(mappedBy = "apu", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)//Un APU puede asignarse a muchas obras
    @JsonManagedReference("apusobra-apu")
    @ToString.Exclude
    private List<ApusObra> apusObraList = new ArrayList<>();

    //`materiales_apu`
    //Relacion muchos a muchos con materiales
    // Relación inversa
    @OneToMany(mappedBy = "apu", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)//Un APU puede contener muchos materiales
    @JsonManagedReference("materialesapu-apu")
    @ToString.Exclude
    private List<MaterialesApu> materialesApus = new ArrayList<>();

    @OneToMany(mappedBy = "idApu", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Avance> avances = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS DE AYUDA ----------
    // Helper para calcular costo total de materiales con proveedor específico
    public BigDecimal calcularCostoMaterialesConProveedor(Long proveedorId) {
        if (materialesApus == null || materialesApus.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return materialesApus.stream()
                .map(materialApu -> {
                    Material material = materialApu.getMaterial();
                    BigDecimal precioUnitario = material.getPrecioPorProveedor(proveedorId);
                    if (precioUnitario == null) {
                        precioUnitario = material.getPrecioActual(); // Fallback al mejor precio
                    }
                    BigDecimal cantidad = BigDecimal.valueOf(materialApu.getCantidad());
                    return precioUnitario.multiply(cantidad);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper para recalcular y actualizar vMaterialesAPU
    public void actualizarCostoMateriales(Long proveedorId) {
        this.vMaterialesAPU = calcularCostoMaterialesConProveedor(proveedorId);
    }

    public void recalcularTotal() {
        this.vTotalApu = vMaterialesAPU
                .add(vManoDeObraAPU != null ? vManoDeObraAPU : BigDecimal.ZERO)
                .add(vTransporteAPU != null ? vTransporteAPU : BigDecimal.ZERO)
                .add(vMiscAPU != null ? vMiscAPU : BigDecimal.ZERO);
    }

    public void agregarMaterial(Material material, Double cantidad) {
        MaterialesApu ma = new MaterialesApu();
        ma.setApu(this);
        ma.setMaterial(material);
        ma.setCantidad(cantidad);
        this.materialesApus.add(ma);
    }

    public void eliminarMaterial(Material material) {
        this.materialesApus.removeIf(ma -> ma.getMaterial().equals(material));
    }

    // ---------- HOOKS DE JPA ----------
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        recalcularTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        recalcularTotal();
    }

}