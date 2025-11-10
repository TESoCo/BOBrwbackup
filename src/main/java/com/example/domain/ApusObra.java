package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

// Separate entity for ACTIVIDADES_OBRA (many-to-many relationship)
@Data
@Entity
@Table(name = "apus_obra")
@IdClass(ApusObraId.class)
public class ApusObra implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Obra")
    @JsonBackReference("apusobra-obra")
    private Obra obra;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_APU")
    @JsonBackReference("apusobra-apu")
    private Apu apu;

    @Column(name = "cantidad", nullable = false)
    private Double cantidad = 1.0; // Default quantity

}
