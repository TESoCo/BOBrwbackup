package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Equipo")
    private Long idEquipo;

    @NotEmpty
    @Column(name = "desc_Equipo")
    private String descEquipo;



    // Agregar relación inversa con Usuario
    @OneToMany(mappedBy = "equipo", fetch = FetchType.LAZY)
    @JsonManagedReference("usuario-equipo")// ← PADRE: se serializa
    @ToString.Exclude
    private List<Usuario> usuarios = new ArrayList<>();

    // Agregar relación inversa con proyecto
    @OneToMany(mappedBy = "equipo", fetch = FetchType.LAZY)
    @JsonManagedReference("equipo-proyectos")
    @ToString.Exclude
    private List<Proyecto> proyectos = new ArrayList<>();



}