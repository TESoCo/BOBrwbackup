package com.example.domain;

import com.example.domain.enums.EstadoInventario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.ToString;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inventario")

@SecondaryTables({
        @SecondaryTable(name = "comentarios_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario")),
        @SecondaryTable(name = "fecha_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario"))
})


public class Inventario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Inventario")
    private Long idInventario;

    // From OBRA_Y_GESTOR_INVENTARIO
    @ManyToOne//un usuario puede crear muchos
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    @ManyToOne//una obra puede crear muchos
    @JoinColumn(name = "id_Obra")
    private Obra idObra;


    @ManyToOne
    @JoinColumn(name = "id_Proveedor")
    private Proveedor proveedorInv;

    @Column(name = "aprobacion_inv")
    @Enumerated(EnumType.STRING) // Esto guarda "SOLICITADO", "APROBADO", etc.
    private EstadoInventario aprobacion = EstadoInventario.SOLICITADO;


    @Column(name = "anular")
    private boolean anular = false;


    // From FECHA_INVENTARIO
    @Column(name = "Fecha_Ingreso", table = "fecha_inventario", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_entrega", table = "fecha_inventario")
    private LocalDate fechaEntrega;

    // From COMENTARIO_INVENTARIO
    @Column(name = "Comentario_Inv", table = "comentarios_inventario", nullable = false)
    private String comentarioInv;

    // Many-to-many relationship for materiales (separate table)
    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();

    // Métodos helpers
    public boolean isAprobado() {
        return aprobacion == EstadoInventario.APROBADO;
    }

    public boolean isEntregado() {
        return aprobacion == EstadoInventario.ENTREGADO;
    }

    public boolean isSolicitado() {
        return aprobacion == EstadoInventario.SOLICITADO;
    }

    public boolean isRechazado() {
        return aprobacion == EstadoInventario.RECHAZADO;
    }

    public boolean isAnulado() {
        return aprobacion == EstadoInventario.ANULADO;
    }

    public boolean isEstadoFinal() {
        return aprobacion.isEstadoFinal();
    }




}
