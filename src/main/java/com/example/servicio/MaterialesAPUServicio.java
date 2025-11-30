package com.example.servicio;

import com.example.domain.MaterialesApu;

import java.util.List;

public interface MaterialesAPUServicio{

    List<MaterialesApu> listarElementos();

    List<MaterialesApu> obtenerPorId(Long id_apu);


    void guardar(MaterialesApu materialesApu);

    void eliminar(MaterialesApu materialesApu);

    // Optional: If you need to search materials
    List<MaterialesApu> buscarPorNombre(String nombre);
}