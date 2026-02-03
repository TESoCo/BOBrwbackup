package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    @ManyToOne
    @JoinColumn(name = "id_Equipo", referencedColumnName = "id_Equipo")
    @JsonBackReference("usuario-equipo")
    private Equipo equipo;

    // Agregar relación inversa con obras
    @OneToMany(mappedBy = "proyecto")
    @JsonManagedReference("proyecto-obra")
    private List<Obra> obras;

    @NotEmpty
    @Column(name = "desc_Proyecto")
    private String descProyecto;
}