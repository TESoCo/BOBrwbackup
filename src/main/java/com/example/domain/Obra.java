// CombinedObraEntity.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.ToString;

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


    // TABLA "OBRA" - INICIO
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Obra")
    private Long idObra;

    @Column(name = "identificador_unico_obra", nullable = false, length = 36)
    private String identificadorUnico; // UUID que agrupa las 3 etapas

    @ManyToOne(fetch = FetchType.LAZY) //un usuario puede crear muchas obras
    @JoinColumn(name = "id_Usuario", referencedColumnName = "id_Usuario")
    @JsonBackReference("usuario-obras")  // ← HIJO: NO se serializa
    @ToString.Exclude
    private Usuario idUsuario;


    @Column(name = "anular")
    private boolean anular;
    // TABLA "OBRA" - FIN


    // TABLA "FECHAS_OBRA" - INICIO
    // From fechas _obra table
    @Column(name = "fecha_Ini", table = "fechas_obra", nullable = false)
    private LocalDate fechaIni;

    @Column(name = "fecha_fin_manual", table = "fechas_obra")
    private LocalDate fechaFinManual;

    @Column(name = "fecha_fin_calculada", table = "fechas_obra")
    private LocalDate fechaFinCalculada;

    // Flag para alertas
    @Column(name = "tiene_alertas", table = "fechas_obra")
    private Boolean tieneAlertas = false;

    // Última fecha de verificación de alertas
    @Column(name = "ultima_verificacion_alertas", table = "fechas_obra")
    private LocalDate ultimaVerificacionAlertas;
    // TABLA "FECHAS_OBRA" - FIN


    // TABLA "UBICACION_OBRA" - INICIO
    // From ubicacion_obra table
    @Column(name = "CooN_Obra", table = "ubicacion_obra", nullable = false)
    private Double cooNObra;

    @Column(name = "CooE_Obra", table = "ubicacion_obra", nullable = false)
    private Double cooEObra;
    // TABLA "UBICACION_OBRA" - FIN


    // TABLA "NOMBRE_OBRA" - INICIO
    // From nombre_obra table
    // Constantes para estados
    public static final String ESTADO_PRESUPUESTO = "PRESUPUESTO";
    public static final String ESTADO_EJECUCION = "EJECUCION";
    public static final String ESTADO_CIERRE = "CIERRE";
    @NotEmpty
    @Column(name = "etapa", table = "nombre_obra", nullable = false)
    private String etapa = ESTADO_PRESUPUESTO;

    @NotEmpty
    @Column(name = "Nombre_Obra", table = "nombre_obra", unique = true)
    private String nombreObra;
    // TABLA "NOMBRE_OBRA" - FIN


    // TABLA "APUS_OBRA" - INICIO
    // Many-to-many relationship for APUS/cantidades (separate table)
    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("apusobra-obra")
    @ToString.Exclude
    private List<ApusObra> apusObraList = new ArrayList<>();
    // TABLA "APUS_OBRA" - FIN


    //Relacion muchos a uno con proyecto, un proyecto puede ser asignado a varios obras
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Proyecto", referencedColumnName = "id_Proyecto")
    @JsonBackReference("proyecto-obras")
    @ToString.Exclude
    private Proyecto proyecto;


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


}


