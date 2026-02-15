package com.example.servicio;

import com.example.domain.Rol;

import java.util.List;

public interface RolServicio {
    Rol buscarPorId(Long id);
    void guardar(Rol rol);
    List<Rol> listarRoles();
    List<Rol> buscarPorNombre(String nombreRol);
    void eliminar(Rol rolBorrar);
}

