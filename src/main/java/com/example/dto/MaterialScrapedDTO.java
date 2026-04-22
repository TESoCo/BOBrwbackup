// NOTA: Esto va en un paquete "dto", NO es un DAO porque no accede a BD

package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de materiales obtenidos por web scraping
 * No es una entidad JPA, solo un contenedor temporal
 */
public class MaterialScrapedDTO {
    private String nombre;
    private String descripcion;
    private String unidad;
    private BigDecimal precio;
    private String proveedorNombre;
    private String proveedorNit;
    private String proveedorCorreo;
    private String proveedorTelefono;
    private String urlFuente;
    private LocalDateTime fechaScraping;
    private String categoria;
    private String marca;

    //Constructor
    public MaterialScrapedDTO() {
        this.fechaScraping = LocalDateTime.now();
    }

    //getters y setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }

    public String getProveedorNit() { return proveedorNit; }
    public void setProveedorNit(String proveedorNit) { this.proveedorNit = proveedorNit; }

    public String getProveedorCorreo() { return proveedorCorreo; }
    public void setProveedorCorreo(String proveedorCorreo) { this.proveedorCorreo = this.proveedorCorreo; }

    public String getProveedorTelefono() { return proveedorTelefono; }
    public void setProveedorTelefono(String proveedorTelefono) { this.proveedorTelefono = proveedorTelefono; }

    public String getUrlFuente() { return urlFuente; }
    public void setUrlFuente(String urlFuente) { this.urlFuente = urlFuente; }

    public LocalDateTime getFechaScraping() { return fechaScraping; }
    public void setFechaScraping(LocalDateTime fechaScraping) { this.fechaScraping = fechaScraping; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
}