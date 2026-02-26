package com.example.dao;

import com.example.domain.MaterialesApu;
import com.example.domain.MaterialesApuId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MaterialesApuDao extends JpaRepository<MaterialesApu, MaterialesApuId>
{// Métodos para clave compuesta
    Optional<MaterialesApu> findByApu_IdAPUAndMaterial_IdMaterial(Long idApu, Long idMaterial);
    boolean existsByApu_IdAPUAndMaterial_IdMaterial(Long idApu, Long idMaterial);

    // Métodos por APU
    List<MaterialesApu> findByApu_IdAPU(Long idApu);

    // Encontrar materiales por ID de APU
    @Query("SELECT ma FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu")
    List<MaterialesApu> findByApuIdApu(@Param("idApu") Long idApu);

    @Modifying
    @Transactional
    void deleteByApu_IdAPU(Long idApu);

    // Métodos por material
    List<MaterialesApu> findByMaterial_IdMaterial(Long idMaterial);

    // Encontrar materiales por nombre de material
    @Query("SELECT ma FROM MaterialesApu ma WHERE ma.material.nombreMaterial LIKE %:nombreMaterial%")
    List<MaterialesApu> findByMaterialNombreMaterialContainingIgnoreCase(@Param("nombreMaterial") String nombreMaterial);

    @Modifying
    @Transactional
    void deleteByMaterial_IdMaterial(Long idMaterial);

    // Métodos por cantidad
    List<MaterialesApu> findByCantidadGreaterThan(Double cantidad);
    List<MaterialesApu> findByCantidadBetween(Double min, Double max);

    // Métodos DELETE corregidos
    @Modifying
    @Transactional
    @Query("DELETE FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu")
    void deleteByApuIdApu(@Param("idApu") Long idApu);

    // Consultas estadísticas
    @Query("SELECT SUM(ma.cantidad) FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu")
    Double sumCantidadByApuId(@Param("idApu") Long idApu);

    @Query("SELECT ma.material.nombreMaterial, SUM(ma.cantidad) FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu GROUP BY ma.material.idMaterial")
    List<Object[]> sumCantidadByMaterialPerApu(@Param("idApu") Long idApu);

    // Búsqueda por múltiples APUs
    @Query("SELECT ma FROM MaterialesApu ma WHERE ma.apu.idAPU IN :idsApu")
    List<MaterialesApu> findByApuIds(@Param("idsApu") List<Long> idsApu);



}
