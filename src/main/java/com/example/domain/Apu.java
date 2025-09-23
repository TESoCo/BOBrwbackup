// CombinedObraEntity.java
package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.poi.hpsf.Decimal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "apu")
@SecondaryTables({
        @SecondaryTable(name = "caracteristicas_apu", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_APU")),
        @SecondaryTable(name = "valor_apu", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_APU")),
})

public class Apu implements Serializable {
    private static final long serialVersionUID = 1L;


    //`apu`
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_APU")
    private Long idAPU;

    @ManyToOne //un usuario puede crear muchos APUs
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;


    //`caracteristicas_apu`
    @NotEmpty
    @Column(name = "nombre", table = "caracteristicas_apu")
    private String nombreAPU;

    @NotEmpty
    @Column(name = "descripcion", table = "caracteristicas_apu")
    private String descAPU;

    @NotEmpty
    @Column(name = "unidad_apu", table = "caracteristicas_apu")
    private String unidadesAPU;


    //`valor_apu`
    @Column(name = "v_APU_Mat", table = "valor_apu")
    private BigDecimal vMaterialesAPU;


    @Column(name = "v_APU_Mano", table = "valor_apu")
    private BigDecimal vManoDeObraAPU;


    @Column(name = "v_APU_Trans", table = "valor_apu")
    private BigDecimal vTransporteAPU;


    @Column(name = "v_APU_Misc", table = "valor_apu")
    private BigDecimal vMiscAPU;

    //apus_obra
    //Relacion muchos a muchos con obra
    // Relación inversa
    // Apu needs the reverse relationship, para agregar apus a las obras
    @OneToMany(mappedBy = "apu", cascade = CascadeType.ALL)//Un APU puede asignarse a muchas obras
    private List<ApusObra> apusObraList = new ArrayList<>();

    //`materiales_apu`
    //Relacion muchos a muchos con materiales
    // Relación inversa
    @OneToMany(mappedBy = "apu", cascade = CascadeType.ALL)//Un APU puede contener muchos materiales
    private List<MaterialesApu> materialesApus = new ArrayList<>();


}