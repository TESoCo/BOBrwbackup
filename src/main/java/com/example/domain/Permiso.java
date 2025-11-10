package com.example.domain;


import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    // LADO INVERSO - usar mappedBy
    @ManyToMany(mappedBy = "permisoList", fetch = FetchType.EAGER)
    @JsonManagedReference("rol-permiso")
    private List<Rol> rolList;
}
