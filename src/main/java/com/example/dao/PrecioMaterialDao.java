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
}