package com.example.servicio;

import com.example.domain.Apu;

import java.util.List;

public interface APUServicio {

    List<Apu> listarElementos();

    Apu obtenerPorId(Long id_apu);

    void guardar(Apu nuevoApu);

    void eliminar(Apu apuBorrar);

    // Optional: If you need to search materials
    List<Apu> buscarPorNombre(String nombre);
}