package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

@Data
@Entity
@Table(name = "avance")

@SecondaryTables({
        @SecondaryTable(name = "fecha_avance", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance")),
        @SecondaryTable(name = "cantidad_avance", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance"))
})

public class Avance implements Serializable{
    private static final long serialVersionUID = 1L;



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avance")
    private Long idAvance;

    // From CONTEXTO_AVANCE
    @ManyToOne//una obra puede crear muchos avances
    @JoinColumn(name = "id_Obra")
    private Obra idObra;

    @ManyToOne
    @JoinColumn(name = "id_Contratista")
    private Contratista idContratista;

    @ManyToOne
    @JoinColumn(name = "id_Usuario")
    private Usuario idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_APU")
    private Apu idApu;


    @Column(name = "anular", unique = true)
    private boolean anular;

    // From FECHA_AVANCE
    @Column(name = "Fecha_Avance", table = "fecha_avance")
    private LocalDate fechaAvance;


    // From CANTIDAD_AVANCE
    @Column(name = "Cant_Ejec", table = "cantidad_avance")
    private Double cantEjec;


}