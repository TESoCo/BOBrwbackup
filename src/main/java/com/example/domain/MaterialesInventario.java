// MaterialesInventario.java
package com.example.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

@Data
@Entity
@Table(name = "materiales_inventario")
@IdClass(MaterialesInventarioId.class)
public class MaterialesInventario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Inventario")
    private Inventario inventario;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Material")
    private Material material;


    @Column(name = "cantidad", nullable = false)
    private double cantidad;

}

