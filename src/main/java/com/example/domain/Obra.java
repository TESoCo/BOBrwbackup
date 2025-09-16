// CombinedObraEntity.java
package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "GESTOR_OBRA")
@SecondaryTables({
        @SecondaryTable(name = "FECHAS_OBRA", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "UBICACION_OBRA", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "NOMBRE_OBRA", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Obra")),
        @SecondaryTable(name = "datos_usuario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Usuario"))
})
public class Obra implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Obra")
    private Integer idObra;

    // From GESTOR_OBRA table
    @ManyToOne
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    // From FECHAS_OBRA table
    @Column(name = "fecha_Ini", table = "FECHAS_OBRA", nullable = false)
    private LocalDate fechaIni;

    @Column(name = "fecha_Fin", table = "FECHAS_OBRA", nullable = false)
    private LocalDate fechaFin;

    // From UBICACION_OBRA table
    @Column(name = "CooN_Obra", table = "UBICACION_OBRA", nullable = false)
    private Double cooNObra;

    @Column(name = "CooE_Obra", table = "UBICACION_OBRA", nullable = false)
    private Double cooEObra;

    // From NOMBRE_OBRA table
    @NotEmpty
    @Column(name = "etapa", table = "NOMBRE_OBRA", unique = true)
    private String etapa;

    @NotEmpty
    @Column(name = "Nombre_Obra", table = "NOMBRE_OBRA")
    private String nombreObra;
}


