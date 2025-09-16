package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

@Data
@Entity
@Table(name = "contexto_avance")

@SecondaryTables({
        @SecondaryTable(name = "FECHA_AVANCE", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance")),
        @SecondaryTable(name = "CANTIDAD_AVANCE", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance")),
        @SecondaryTable(name = "avance", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Avance")
        )
        
})

public class Avance implements Serializable{
    private static final long serialVersionUID = 1L;



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avance")
    private Integer idAvance;

    // From CONTEXTO_AVANCE
    @ManyToOne
    @JoinColumn(name = "id_Obra")
    private Obra idObra;

    @ManyToOne
    @JoinColumn(name = "id_Contratista")
    private Contratista idContratista;

    @ManyToOne
    @JoinColumn(name = "id_Usuario", nullable = false)
    private Usuario idUsuario;


    @ManyToOne
    @JoinColumn(name = "id_APU")
    private Apu apu;

    // From FECHA_AVANCE
    @Column(name = "Fecha_Avance", table = "FECHA_AVANCE")
    private LocalDate fechaAvance;


    // From CANTIDAD_AVANCE
    @Column(name = "Cant_Ejec", table = "CANTIDAD_AVANCE")
    private Double cantEjec;



}