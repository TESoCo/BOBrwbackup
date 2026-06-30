package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "proyecto")
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Proyecto")
    private Long idProyecto;

    //Relacion muchos a uno con equipo, un equipo puede ser asignado a varios proyectos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Equipo", referencedColumnName = "id_Equipo")
    @JsonBackReference("equipo-proyectos")  // ← HIJO: NO se serializa
    @ToString.Exclude
    private Equipo equipo;

    // RELACIÓN CON OBRAS
    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    @JsonManagedReference("proyecto-obras")  // ← PADRE: se serializa
    @ToString.Exclude
    private List<Obra> obras = new ArrayList<>();

    @NotEmpty
    @Column(name = "desc_Proyecto")
    private String descProyecto;
}