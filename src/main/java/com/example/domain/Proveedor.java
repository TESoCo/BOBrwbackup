package com.example.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "proveedor",
        indexes = {
                @Index(name = "idx_proveedor_nombre", columnList = "nombre_proveedor"),
                @Index(name = "idx_proveedor_persona", columnList = "id_persona_contacto"),
                @Index(name = "idx_proveedor_info_comercial", columnList = "id_info_comercial")
        })

public class Proveedor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Proveedor")
    private Long idProveedor;

    @NotEmpty(message = "El nombre del proveedor es obligatorio")
    @Column(name = "nombre_proveedor", nullable = false, length = 255)
    private String nombreProveedor;

    @Column(name = "codigo_proveedor", length = 50, unique = true)
    private String codigoProveedor;


    // ---------- RELACIONES ----------
    // One Proveedor has One primary contact Persona
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona_contacto", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Persona idPersona;

    // Commercial Information for the SUPPLIER (the company itself)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_info_comercial", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private InformacionComercial informacionComercial;

    // From proveedores_material (proveedor ID) Relación ManyToMany con materiales
    @ManyToMany(fetch = FetchType.LAZY)//  Tipo de relación y carga
    @JoinTable(//  Define la tabla intermedia
            name = "proveedores_material", //  Nombre de la tabla junction
            joinColumns = @JoinColumn(name = "id_Proveedor"),//  Columna de esta entidad
            inverseJoinColumns = @JoinColumn(name = "id_Material") //  Columna de la otra entidad
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Material> materialList  = new ArrayList<>();

    //Precios que este proveedor ofrece
    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("proveedor-precios")
    @ToString.Exclude
    private List<PrecioMaterial> preciosOfrecidos = new ArrayList<>();

    @OneToMany(mappedBy = "proveedorInv", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Inventario> inventarios = new ArrayList<>();


    // ---------- AUDITORÍA ----------
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ---------- MÉTODOS DE AYUDA ----------
    public void agregarMaterial(Material material) {
        if (!materialList.contains(material)) {
            materialList.add(material);
        }
    }

    public void eliminarMaterial(Material material) {
        materialList.remove(material);
    }

    public BigDecimal getPrecioMaterial(Material material) {
        return preciosOfrecidos.stream()
                .filter(p -> p.getMaterial().equals(material))
                .filter(PrecioMaterial::estaVigente)
                .findFirst()
                .map(PrecioMaterial::getPrecioUnitario)
                .orElse(null);
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (codigoProveedor == null) {
            codigoProveedor = "PROV-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
