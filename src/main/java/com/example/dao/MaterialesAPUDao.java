package com.example.dao;

import com.example.domain.MaterialesApu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MaterialesAPUDao  extends JpaRepository<MaterialesApu, Long>
{

    // Encontrar materiales por ID de APU
    @Query("SELECT ma FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu")
    List<MaterialesApu> findByApuIdApu(@Param("idApu") Long idApu);

    // Encontrar materiales por nombre de material
    @Query("SELECT ma FROM MaterialesApu ma WHERE ma.material.nombreMaterial LIKE %:nombreMaterial%")
    List<MaterialesApu> findByMaterialNombreMaterialContainingIgnoreCase(@Param("nombreMaterial") String nombreMaterial);

    // Eliminar materiales por ID de APU
    @Query("DELETE FROM MaterialesApu ma WHERE ma.apu.idAPU = :idApu")
    void deleteByApuIdApu(@Param("idApu") Long idApu);

}
