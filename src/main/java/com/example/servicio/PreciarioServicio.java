package com.example.servicio;

import com.example.domain.Preciario;
import com.example.domain.Apu;
import java.util.List;

public interface PreciarioServicio {
    List<Preciario> listarTodos();
    List<Preciario> listarActivos();
    List<Preciario> buscarPorNombre(String nombre);
    Preciario obtenerPorId(Long id);
    Preciario guardar(Preciario preciario);
    void eliminar(Preciario preciario);
    void agregarApu(Long idPreciario, Long idApu, Integer orden);
    void eliminarApu(Long idPreciario, Long idApu);
    List<Apu> obtenerApusDePreciario(Long idPreciario);
}