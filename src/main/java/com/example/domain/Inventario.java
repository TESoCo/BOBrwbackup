package com.example.domain;

import com.example.domain.enums.EstadoInventario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "inventario",
        indexes = {
                @Index(name = "idx_inventario_obra", columnList = "id_obra"),
                @Index(name = "idx_inventario_usuario", columnList = "id_usuario_solicitante"),
                @Index(name = "idx_inventario_estado", columnList = "estado"),
                @Index(name = "idx_inventario_fecha_ingreso", columnList = "fecha_ingreso")
        })


public class Inventario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Inventario")
    private Long idInventario;


    // ---------- RELACIONES ----------
    // From OBRA_Y_GESTOR_INVENTARIO
    @ManyToOne(fetch = FetchType.LAZY)//un usuario puede crear muchos
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Usuario idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)//una obra puede crear muchos
    @JoinColumn(name = "id_obra", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Obra idObra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor")
    @JsonIgnore
    @ToString.Exclude
    private Proveedor proveedorInv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprobador")
    @JsonIgnore
    @ToString.Exclude
    private Usuario usuarioAprobador;


    // ---------- DATOS DEL INVENTARIO ----------
    @NotNull(message = "El estado es obligatorio")
    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING) // Esto guarda "SOLICITADO", "APROBADO", etc.
    private EstadoInventario aprobacion = EstadoInventario.SOLICITADO;


    // From FECHA_INVENTARIO
    @NotNull(message = "La fecha de ingreso es obligatoria")
    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    // From COMENTARIO_INVENTARIO
    @NotEmpty(message = "El comentario es obligatorio")
    @Column(name = "comentario", nullable = false, columnDefinition = "TEXT")
    private String comentarioInv;

    @Column(name = "numero_solicitud", length = 50, unique = true)
    private String numeroSolicitud;

    @Column(name = "anular")
    private boolean anular = false;


    // ---------- RELACIONES CON MATERIALES ----------
    // Many-to-many relationship for materiales (separate table)
    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


    // ---------- MÉTODOS DE AYUDA ----------
    public void agregarMaterial(Material material, Double cantidad) {
        MaterialesInventario mi = new MaterialesInventario();
        mi.setInventario(this);
        mi.setMaterial(material);
        mi.setCantidad(cantidad);
        this.materialesInventarios.add(mi);
    }

    public void eliminarMaterial(Material material) {
        this.materialesInventarios.removeIf(mi -> mi.getMaterial().equals(material));
    }

    public boolean cambiarEstado(EstadoInventario nuevoEstado, Usuario usuario, String comentario) {
        if (!this.aprobacion.isTransicionValida(nuevoEstado)) {
            return false;
        }

        // Registrar auditoría
        String estadoAnterior = this.aprobacion.getDisplayName();
        this.aprobacion = nuevoEstado;

        if (nuevoEstado == EstadoInventario.APROBADO) {
            this.usuarioAprobador = usuario;
        }

        return true;
    }

    public boolean isEstadoFinal() {
        return this.aprobacion.equals(EstadoInventario.ENTREGADO) || this.aprobacion.equals(EstadoInventario.RECHAZADO) || this.aprobacion.equals(EstadoInventario.ANULADO);
    }

    // ---------- HOOKS JPA ----------
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (numeroSolicitud == null) {
            numeroSolicitud = "SOL-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

}
