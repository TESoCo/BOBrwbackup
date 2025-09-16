package com.example.servicio;

import com.example.domain.Material;

import java.util.List;

public interface MaterialServicio {

    List<Material> listarTodos();
    Material obtenerPorId(Integer idMaterial);
    void guardar(Material material);
    void eliminar(Integer idMaterial);
}
