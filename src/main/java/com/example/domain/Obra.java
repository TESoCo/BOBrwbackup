// CombinedObraEntity.java
package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "obra")
@SecondaryTables({
        @SecondaryTable(name = "fechas_obra", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "ubicacion_obra", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "nombre_obra", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "datos_usuario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Usuario"))
})
public class Obra implements Serializable {
    private static final long serialVersionUID = 1L;


    // From OBRA table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Obra")
    private Long idObra;

    @ManyToOne //un usuario puede crear muchas obras
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    @NotEmpty
    @Column(name = "anular", unique = true)
    private boolean anular;


    // From fechas _obra table
    @Column(name = "fecha_Ini", table = "fechas_obra", nullable = false)
    private LocalDate fechaIni;

    @Column(name = "fecha_Fin", table = "fechas_obra", nullable = false)
    private LocalDate fechaFin;


    // From ubicacion_obra table
    @Column(name = "CooN_Obra", table = "ubicacion_obra", nullable = false)
    private Double cooNObra;

    @Column(name = "CooE_Obra", table = "ubicacion_obra", nullable = false)
    private Double cooEObra;


    // From nombre_obra table
    @NotEmpty
    @Column(name = "etapa", table = "nombre_obra", unique = true)
    private String etapa;

    @NotEmpty
    @Column(name = "Nombre_Obra", table = "nombre_obra")
    private String nombreObra;

    // From apus_obra table
    // Many-to-many relationship for APUS/cantidades (separate table)
    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApusObra> apusObraList = new ArrayList<>();

}


