package com.example.servicio;

import com.example.domain.Rol;

import java.util.List;

public interface RolServicio {
    Rol buscarPorId(Integer id);
    void guardar(Rol rol);
    List<Rol> listarRoles();
}

