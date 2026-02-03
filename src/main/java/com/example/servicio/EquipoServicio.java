package com.example.servicio;

import com.example.domain.Equipo;

import java.util.List;

public interface EquipoServicio {
    List<Equipo> listarEquipos();
    Equipo encontrarPorId(Long id);
    void guardar(Equipo equipo);
    void borrar(Equipo equipo);
    List<Equipo> buscarPorDescripcion(String descripcion);
    List<Equipo> buscarEquiposSinUsuarios();
    List<Equipo> buscarEquiposSinProyectos();
}