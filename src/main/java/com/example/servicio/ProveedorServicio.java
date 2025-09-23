package com.example.servicio;

import com.example.domain.Contratista;
import com.example.domain.Persona;
import com.example.domain.Proveedor;

import java.util.List;

public interface ProveedorServicio {
    public List<Proveedor> listarProveedores();
    public void guardar(Proveedor proveedor);
    public void borrar(Proveedor proveedor);
    public Proveedor encontrarPorId(Long id);
    Proveedor encontrarPorIdPersona(Persona idPersona);

}