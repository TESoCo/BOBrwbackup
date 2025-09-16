package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "obra_y_gestor_inventario")

@SecondaryTables({
        @SecondaryTable(name = "unidades_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario")),
        @SecondaryTable(name = "cantidades_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario")),
        @SecondaryTable(name = "fecha_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Inventario"))
})


public class Inventario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Inventario")
    private Long idInventario;

    // From OBRA_Y_GESTOR_INVENTARIO
    @ManyToOne
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_Obra")
    private Obra idObra;

 //   @NotEmpty
  //  private String tipoRegistro;
//lo reemplaza la clase "consumo"

    // From FECHA_INVENTARIO
    @Column(name = "Fecha_Ingreso", table = "FECHA_INVENTARIO", nullable = false)
    private LocalDate fechaIngreso;

    // From CANTIDADES_INVENTARIO
    @Column(name = "Cantidad_Mat", table = "CANTIDADES_INVENTARIO", nullable = false)
    private Double cantidadMat;

    // From UNIDADES_INVENTARIO
    @Column(name = "Unidad_Inv", table = "UNIDADES_INVENTARIO", nullable = false)
    private String unidadInv;

    // Many-to-many relationship for materiales (separate table)
    @OneToMany(mappedBy = "id_Inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialesInventario> materiales = new ArrayList<>();

}
