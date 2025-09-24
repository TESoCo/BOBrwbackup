package com.example.servicio;

import com.example.domain.Permiso;

import java.util.List;

public interface PermisoServicio {
    List<Permiso> listarPermisos();
    void guardar(Permiso permiso);
    List<Permiso> buscarPorNombre(String nombre);
}
