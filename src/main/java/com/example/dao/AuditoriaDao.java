package com.example.dao;

import com.example.domain.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaDao extends JpaRepository<Auditoria, Long> {

    // Buscar auditorías por entidad y ID de entidad
    List<Auditoria> findByEntidadAndIdEntidadOrderByFechaCambioDesc(String entidad, Long idEntidad);

    // Buscar auditorías por entidad
    List<Auditoria> findByEntidadOrderByFechaCambioDesc(String entidad);

    // Buscar auditorías por usuario
    List<Auditoria> findByUsuario_IdUsuarioOrderByFechaCambioDesc(Long idUsuario);

    // Buscar auditorías por acción
    List<Auditoria> findByAccionOrderByFechaCambioDesc(Auditoria.AccionAuditoria accion);

    // Buscar auditorías por rango de fechas
    List<Auditoria> findByFechaCambioBetween(LocalDateTime start, LocalDateTime end);

    // Buscar auditorías de una entidad específica con un campo específico
    List<Auditoria> findByEntidadAndIdEntidadAndCampoOrderByFechaCambioDesc(
            String entidad, Long idEntidad, String campo);

    // Buscar auditorías de inventario por estado
    @Query("SELECT a FROM Auditoria a WHERE a.entidad = 'INVENTARIO' AND a.idEntidad = :idInventario ORDER BY a.fechaCambio DESC")
    List<Auditoria> findAuditoriasByInventarioId(@Param("idInventario") Long idInventario);

    // Obtener la última auditoría de una entidad
    @Query("SELECT a FROM Auditoria a WHERE a.entidad = :entidad AND a.idEntidad = :idEntidad ORDER BY a.fechaCambio DESC")
    List<Auditoria> findLastAuditoriaByEntidadAndIdEntidad(
            @Param("entidad") String entidad,
            @Param("idEntidad") Long idEntidad);
}