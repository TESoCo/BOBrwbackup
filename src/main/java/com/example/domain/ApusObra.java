package com.example.domain;

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
    private Obra obra;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_APU")
    private Apu apu;

    @Column(name = "cantidad", nullable = false)
    private Double cantidad = 1.0; // Default quantity

}
