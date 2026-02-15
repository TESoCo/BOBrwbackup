package com.example.servicio;

import com.example.domain.Proyecto;

import java.util.List;

public interface ProyectoServicio {
    List<Proyecto> listarProyectos();
    Proyecto encontrarPorId(Long id);
    void guardar(Proyecto proyecto);
    void borrar(Proyecto proyecto);
    List<Proyecto> buscarPorDescripcion(String descripcion);
    List<Proyecto> buscarPorEquipo(Long idEquipo);
    List<Proyecto> buscarProyectosSinEquipo();
    List<Proyecto> buscarProyectosConObras();
}