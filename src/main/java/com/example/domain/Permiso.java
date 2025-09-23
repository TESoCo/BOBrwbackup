package com.example.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Table(name="permiso")
public class Permiso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Permiso")
    private Long idPermiso;

    @NotEmpty
    @Column(name = "nombre_Permiso", unique = true)
    private String nombrePermiso;

    //Tabla "rol_permiso"
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_Permiso"),
            inverseJoinColumns = @JoinColumn(name = "id_Rol")
    )
    private List<Rol> rolList;

}
