// CombinedObraEntity.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "obra",
        indexes = {
                @Index(name = "idx_obra_usuario", columnList = "id_usuario_creador"),
                @Index(name = "idx_obra_proyecto", columnList = "id_proyecto"),
                @Index(name = "idx_obra_etapa", columnList = "etapa"),
                @Index(name = "idx_obra_identificador", columnList = "identificador_unico")
        })
public class Obra implements Serializable {
    private static final long serialVersionUID = 1L;

    // ---------- ENUM PARA ETAPAS ----------
    public enum EtapaObra {
        PRESUPUESTO, EJECUCION, CIERRE
    }


    // TABLA "OBRA" - INICIO
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Obra")
    private Long idObra;

    @Column(name = "identificador_unico", nullable = false, length = 36, unique = true)
    private String identificadorUnico; // UUID que agrupa las 3 etapas


    // ---------- DATOS DE LA OBRA ----------
    @NotEmpty(message = "El nombre de la obra es obligatorio")
    @Column(name = "nombre_obra", nullable = false, length = 255, unique = true)
    private String nombreObra;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotNull(message = "La etapa es obligatoria")
    @Column(name = "etapa", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EtapaObra etapa = EtapaObra.PRESUPUESTO;


    // ---------- FECHAS ----------
    // From fechas _obra table
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaIni;

    @Column(name = "fecha_fin_manual")
    private LocalDate fechaFinManual;

    @Column(name = "fecha_fin_calculada")
    private LocalDate fechaFinCalculada;

    // Flag para alertas
    @Column(name = "tiene_alertas")
    private Boolean tieneAlertas = false;

    // Última fecha de verificación de alertas
    @Column(name = "ultima_verificacion_alertas")
    private LocalDate ultimaVerificacionAlertas;


    // ---------- UBICACIÓN ----------
    // From ubicacion_obra table
    @NotNull(message = "La coordenada norte es obligatoria")
    @Column(name = "coordenada_norte", nullable = false)
    private Double cooNObra;

    @NotNull(message = "La coordenada este es obligatoria")
    @Column(name = "coordenada_este", nullable = false)
    private Double cooEObra;


    // ---------- VALORES ----------
    @Column(name = "presupuesto_total", precision = 20, scale = 2)
    private BigDecimal presupuestoTotal = BigDecimal.ZERO;

    @Column(name = "costo_acumulado", precision = 20, scale = 2)
    private BigDecimal costoAcumulado = BigDecimal.ZERO;

    @Column(name = "porcentaje_avance")
    private Double porcentajeAvance = 0.0;


    // ---------- RELACIONES ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Usuario idUsuario;

    //Relacion muchos a uno con proyecto, un proyecto puede ser asignado a varios obras
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto")
    @JsonIgnore
    @ToString.Exclude
    private Proyecto proyecto;

    // Many-to-many relationship for APUS/cantidades (separate table)
    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("apusobra-obra")
    @ToString.Exclude
    private List<ApusObra> apusObraList = new ArrayList<>();

    @OneToMany(mappedBy = "idObra", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Avance> avances = new ArrayList<>();

    @OneToMany(mappedBy = "idObra", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Inventario> inventarios = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "anular")
    private boolean anular = false;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS DE AYUDA ----------
    public void agregarApu(Apu apu, Double cantidad) {
        ApusObra apusObra = new ApusObra(this, apu, cantidad);
        this.apusObraList.add(apusObra);
    }

    public void eliminarApu(Apu apu) {
        this.apusObraList.removeIf(ao -> ao.getApu().equals(apu));
    }

    // Métod0 para clonar la estructura de APUs de otra obra
    public void clonarEstructuraDe(Obra otraObra) {
        if (this.getApusObraList() == null) {
            this.setApusObraList(new ArrayList<>());
        }
        this.getApusObraList().clear();

        for (ApusObra apusObra : otraObra.getApusObraList()) {
            ApusObra nuevoApusObra = new ApusObra();
            nuevoApusObra.setApu(apusObra.getApu());
            nuevoApusObra.setObra(this);
            nuevoApusObra.setCantidad(0.0); // Comienza en 0 para EJECUCIÓN o CIERRE
            this.getApusObraList().add(nuevoApusObra);
        }
    }

    public BigDecimal calcularCostoTotal(Long proveedorId) {
        return apusObraList.stream()
                .map(ao -> {
                    BigDecimal costoUnitario = ao.getApu()
                            .calcularCostoMaterialesConProveedor(proveedorId);
                    return costoUnitario.multiply(BigDecimal.valueOf(ao.getCantidad()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalcularPorcentajeAvance() {
        if (apusObraList == null || apusObraList.isEmpty()) {
            this.porcentajeAvance = 0.0;
            return;
        }

        double totalApus = apusObraList.size();
        double apusConAvance = apusObraList.stream()
                .filter(ao -> ao.getCantidad() > 0)
                .count();

        this.porcentajeAvance = (apusConAvance / totalApus) * 100;
    }

    // ---------- HOOKS JPA ----------
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (identificadorUnico == null) {
            identificadorUnico = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        recalcularPorcentajeAvance();
    }


}


