package com.example.dao;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.MaterialesApu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MaterialDao extends JpaRepository<Material, Long> {

    // Búsqueda por nombre
    List<Material> findByNombreMaterialContainingIgnoreCase(String nombre);
    List<Material> findByNombreMaterialStartingWithIgnoreCase(String nombre);
    Optional<Material> findByNombreMaterial(String nombre);

    // Búsqueda por unidad
    List<Material> findByUnidadMaterial(String unidad);
    List<Material> findByUnidadMaterialIn(List<String> unidades);

    // Búsqueda por descripción
    List<Material> findByDescripcionMaterialContainingIgnoreCase(String descripcion);

    // Búsqueda por precio
    List<Material> findByPrecioMaterialBetween(BigDecimal min, BigDecimal max);
    List<Material> findByPrecioMaterialGreaterThanEqual(BigDecimal min);
    List<Material> findByPrecioMaterialLessThanEqual(BigDecimal max);

    // Búsqueda por proveedor
    @Query("SELECT m FROM Material m JOIN m.proveedorList p WHERE p.idProveedor = :idProveedor")
    List<Material> findByProveedorId(@Param("idProveedor") Long idProveedor);

    @Query("SELECT m FROM Material m JOIN m.proveedorList p WHERE p.idPersona.nombre LIKE %:nombre%")
    List<Material> findByProveedorNombreContaining(@Param("nombre") String nombre);

    // Búsqueda por APU
    @Query("SELECT m FROM Material m JOIN m.materialesApus ma WHERE ma.apu.idAPU = :idApu")
    List<Material> findByApuId(@Param("idApu") Long idApu);

    // Búsqueda por inventario
    @Query("SELECT m FROM Material m JOIN m.materialesInventarios mi WHERE mi.inventario.idInventario = :idInventario")
    List<Material> findByInventarioId(@Param("idInventario") Long idInventario);

    // Conteo y estadísticas
    Long countByUnidadMaterial(String unidad);

    @Query("SELECT AVG(m.precioMaterial) FROM Material m WHERE m.unidadMaterial = :unidad")
    BigDecimal avgPrecioByUnidad(@Param("unidad") String unidad);

    // Ordenamiento
    List<Material> findAllByOrderByNombreMaterialAsc();
    List<Material> findByUnidadMaterialOrderByPrecioMaterialAsc(String unidad);

    // Búsqueda combinada
    List<Material> findByNombreMaterialContainingIgnoreCaseAndUnidadMaterial(String nombre, String unidad);
    List<Material> findByPrecioMaterialBetweenAndUnidadMaterialIn(BigDecimal min, BigDecimal max, List<String> unidades);

}
