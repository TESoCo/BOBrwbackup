package com.example.dao;


import com.example.domain.Inventario;
import com.example.domain.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventarioDao extends JpaRepository<Inventario, Long> {

    List<Inventario> findByIdUsuario_idUsuario(Long IdUsuario);
    List<Inventario> findByIdObra(Obra obra);

    List<Inventario> findByFechaIngreso(LocalDate fecha);
    List<Inventario> findByFechaIngresoBetween(LocalDate start, LocalDate end);
    List<Inventario> findByFechaIngresoBefore(LocalDate date);
    List<Inventario> findByFechaIngresoAfter(LocalDate date);

    List<Inventario> findByTipoInv(String tipoInv);
    List<Inventario> findByTipoInvIn(List<String> tipos);

    List<Inventario> findByUnidadInv(String unidad);
    List<Inventario> findByUnidadInvContainingIgnoreCase(String unidad);

    List<Inventario> findByAnular(boolean anular);

    List<Inventario> findByIdObra_IdObraAndTipoInv(Long idObra, String tipoInv);
    List<Inventario> findByIdUsuario_IdUsuarioAndFechaIngresoBetween(Long idUsuario, LocalDate start, LocalDate end);

    // Estadísticas
    @Query("SELECT COUNT(DISTINCT i.idObra) FROM Inventario i WHERE i.tipoInv = :tipo")
    Long countObrasByTipoInv(@Param("tipo") String tipo);

    // Ordenamiento
    List<Inventario> findByTipoInvOrderByFechaIngresoDesc(String tipoInv);
    List<Inventario> findByIdObra_IdObraOrderByFechaIngresoAsc(Long idObra);

}
