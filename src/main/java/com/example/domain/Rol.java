package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Table (name = "rol")
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    //tabla "rol"
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Rol")
    private Long idRol;

    @NotEmpty
    @Column(name = "nombre_Rol", unique = true)
    private String nombreRol;

    @NotEmpty
    @Column(name = "descrip_Rol", unique = true)
    private String descripRol;


    //Tabla "rol_permiso"
    // LADO PROPIETARIO - mantener JoinTable
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_Rol"),
            inverseJoinColumns = @JoinColumn(name = "id_Permiso")
    )
    @JsonBackReference("rol-permiso")
    private List<Permiso> permisoList;

    // Agregar relación inversa con Usuario
    @OneToMany(mappedBy = "rol")
    @JsonManagedReference("usuario-rol")
    private List<Usuario> usuarios;


}
