package com.example.dao;


import com.example.domain.Inventario;
import com.example.domain.Obra;
import com.example.domain.enums.EstadoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InventarioDao extends JpaRepository<Inventario, Long> {

    List<Inventario> findByIdUsuario_idUsuario(Long IdUsuario);
    List<Inventario> findByIdObra(Obra obra);

    List<Inventario> findByFechaIngreso(LocalDate fecha);
    List<Inventario> findByFechaIngresoBetween(LocalDate start, LocalDate end);
    List<Inventario> findByFechaIngresoBefore(LocalDate date);
    List<Inventario> findByFechaIngresoAfter(LocalDate date);

    List<Inventario> findByUnidadInv(String unidad);
    List<Inventario> findByUnidadInvContainingIgnoreCase(String unidad);

    List<Inventario> findByAnular(boolean anular);


    List<Inventario> findByIdUsuario_IdUsuarioAndFechaIngresoBetween(Long idUsuario, LocalDate start, LocalDate end);

    List<Inventario> findByAprobacion(EstadoInventario aprobacion);


    // Ordenamiento
    List<Inventario> findByAprobacionOrderByFechaIngresoDesc(EstadoInventario aprobacion);
    List<Inventario> findByIdObra_IdObraOrderByFechaIngresoAsc(Long idObra);

}
