package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "materiales_apu")
@Data
@IdClass(MaterialesApuId.class) // Clase para la clave compuesta
public class MaterialesApu implements Serializable {
    private static final long serialVersionUID = 1L;


    // Relación Many-to-One con APU
    @Id
    @ManyToOne
    @JoinColumn(name = "id_APU")
    @JsonBackReference("materialesapu-apu")
    private Apu apu;

    // Relación Many-to-One con material
    @Id
    @ManyToOne
    @JoinColumn(name = "id_Material")
    @JsonBackReference("materialesapu-material")
    private Material material;


    @Column(name = "cantidad")
    private Double cantidad;


}
