package com.example.servicio;

import com.example.domain.Apu;
import com.example.domain.MaterialesApu;

import java.util.List;

public interface MaterialesAPUServicio {

    List<MaterialesApu> listarElementos();

    Apu obtenerPorId(Integer id_apu);

    void guardar(MaterialesApu materialesApu);

    void eliminar(MaterialesApu materialesApu);

    // Optional: If you need to search materials
    List<Apu> buscarPorNombre(String nombre);
}