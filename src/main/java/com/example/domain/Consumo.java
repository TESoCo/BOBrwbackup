// Consumo.java
package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "contexto_consumo")
@SecondaryTable(name = "fecha_consumo", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_Consumo"))
public class Consumo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Consumo")
    private Integer idConsumo;

    // From CONTEXTO_CONSUMO
    @ManyToOne
    @JoinColumn(name = "id_Inventario")
    private ObraYGestorInventario inventario;

    @ManyToOne
    @JoinColumn(name = "id_Avance")
    private Avance idAvance;

    // From FECHA_CONSUMO
    @Column(name = "Fecha_Cons", table = "fecha_consumo")
    private LocalDate fechaCons;
}