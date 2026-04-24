// NOTA: Esto va en un paquete "dto", NO es un DAO porque no accede a BD

package com.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de materiales obtenidos por web scraping
 * No es una entidad JPA, solo un contenedor temporal
 */
@Getter
@Setter
public class MaterialScrapedDTO {
    private String nombre;
    private String descripcion;
    private String unidad;

    private String proveedorNombre;
    private String proveedorNit;
    private String proveedorCorreo;
    private String proveedorTelefono;
    private String urlFuente;
    private LocalDateTime fechaScraping;
    private String categoria;
    private String marca;
    private BigDecimal precio;

    //Constructor
    public MaterialScrapedDTO() {
        this.fechaScraping = LocalDateTime.now();
    }


}