// CombinedObraEntity.java
package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "gestor_apu")
@SecondaryTables({
        @SecondaryTable(name = "caracteristicas_apu", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_APU")),
        @SecondaryTable(name = "valor_apu", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_APU")),
        @SecondaryTable(name = "materiales_inventario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_APU")),
        @SecondaryTable(name = "datos_usuario", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Usuario"))
})

public class Apu implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_APU")
    private Integer idAPU;

    @ManyToOne
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    //7b. CARACTERÍSTICAS APU
    @NotEmpty
    @Column(name = "nombre", table = "caracteristicas_apu")
    private String nombreAPU;


    @Column(name = "descripcion", table = "caracteristicas_apu")
    private String descAPU;

    @NotEmpty
    @Column(name = "unidad_apu", table = "caracteristicas_apu")
    private String unidadesAPU;


    @Column(name = "v_APU_Mat", table = "valor_apu")
    private String vMaterialesAPU;


    @Column(name = "v_APU_Mano", table = "valor_apu")
    private String vManoDeObraAPU;


    @Column(name = "v_APU_Trans", table = "valor_apu")
    private String vTransporteAPU;


    @Column(name = "v_APU_Misc", table = "valor_apu")
    private String vMiscAPU;

    @ManyToOne
    @JoinColumn(name = "id_Material")
    private Material material;

}