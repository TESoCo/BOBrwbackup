// MaterialesInventario.java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore // Prevent circular reference
    private Inventario inventario;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_Material")
    @JsonIgnore // Prevent circular reference
    private Material material;


    @Column(name = "cantidad", nullable = false)
    private double cantidad;

}

