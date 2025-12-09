package com.example.domain;

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
        @SecondaryTable(name = "unidades_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario")),
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


    @Column(name = "tipo_inv")
    private String tipoInv = "INGRESO";


    @Column(name = "anular")
    private boolean anular = false;


    // From FECHA_INVENTARIO
    @Column(name = "Fecha_Ingreso", table = "fecha_inventario", nullable = false)
    private LocalDate fechaIngreso;

    // From UNIDADES_INVENTARIO
    @Column(name = "Unidad_Inv", table = "unidades_inventario", nullable = false)
    private String unidadInv;

    // Many-to-many relationship for materiales (separate table)
    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<MaterialesInventario> materialesInventarios = new ArrayList<>();



}
