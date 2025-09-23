package com.example.servicio;

import com.example.domain.Material;
import org.springframework.stereotype.Service;

import java.util.List;


public interface MaterialServicio {

    List<Material> listarTodos();
    Material obtenerPorId(Long idMaterial);
    void guardar(Material material);
    void eliminar(Long idMaterial);
}
