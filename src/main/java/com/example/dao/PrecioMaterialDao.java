package com.example.dao;

import com.example.domain.PrecioMaterial;
import com.example.domain.PrecioMaterialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PrecioMaterialDao extends JpaRepository<PrecioMaterial, PrecioMaterialId> {

    // Obtener precio activo de un material para un proveedor específico
    Optional<PrecioMaterial> findByMaterial_IdMaterialAndProveedor_IdProveedorAndActivoTrue(
            Long materialId, Long proveedorId);

    // Obtener todos los precios activos de un material
    List<PrecioMaterial> findByMaterial_IdMaterialAndActivoTrue(Long materialId);

    // Obtener el mejor precio actual (más bajo) de un material
    @Query("SELECT MIN(p.precioUnitario) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Optional<BigDecimal> findMejorPrecioByMaterialId(@Param("materialId") Long materialId);

    // Obtener todos los precios de un proveedor
    List<PrecioMaterial> findByProveedor_IdProveedorAndActivoTrue(Long proveedorId);

    // Verificar si existe precio activo
    boolean existsByMaterial_IdMaterialAndProveedor_IdProveedorAndActivoTrue(
            Long materialId, Long proveedorId);

    // ========== ESTADÍSTICAS DE PRECIOS POR MATERIAL ==========

    // Precio promedio de un material (entre todos sus proveedores activos)
    @Query("SELECT AVG(p.precioUnitario) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Optional<BigDecimal> findPrecioPromedioByMaterialId(@Param("materialId") Long materialId);

    // Precio máximo de un material (entre todos sus proveedores activos)
    @Query("SELECT MAX(p.precioUnitario) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Optional<BigDecimal> findPrecioMaximoByMaterialId(@Param("materialId") Long materialId);

    // Cantidad de proveedores que ofrecen un material con precio activo
    @Query("SELECT COUNT(DISTINCT p.proveedor.idProveedor) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Long countProveedoresByMaterialId(@Param("materialId") Long materialId);

    // Desviación estándar de precios (qué tan dispersos están)
    @Query("SELECT STDDEV(p.precioUnitario) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Optional<Double> findDesviacionEstandarPreciosByMaterialId(@Param("materialId") Long materialId);

    // Rango de precios (máximo - mínimo)
    @Query("SELECT MAX(p.precioUnitario) - MIN(p.precioUnitario) FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP)")
    Optional<BigDecimal> findRangoPreciosByMaterialId(@Param("materialId") Long materialId);

    // Obtener todos los precios únicos ordenados (para análisis)
    @Query("SELECT DISTINCT p.precioUnitario FROM PrecioMaterial p " +
            "WHERE p.material.idMaterial = :materialId AND p.activo = true " +
            "AND (p.fechaVigenciaHasta IS NULL OR p.fechaVigenciaHasta > CURRENT_TIMESTAMP) " +
            "ORDER BY p.precioUnitario ASC")
    List<BigDecimal> findPreciosOrdenadosByMaterialId(@Param("materialId") Long materialId);
}