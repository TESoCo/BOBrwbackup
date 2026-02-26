package com.example.dao;

import com.example.domain.Apu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;


public interface ApuDao extends JpaRepository<Apu, Long> {

    List<Apu> findByNombreAPUContainingIgnoreCase(String nombreAPU);
    List<Apu> findByNombreAPUStartingWithIgnoreCase(String nombreAPU);
    List<Apu> findByNombreAPU(String nombreAPU);

    // Búsqueda por usuario
    List<Apu> findByIdUsuario_IdUsuario(Long idUsuario);

    // Búsqueda por rangos de valor - CON @Query
    @Query("SELECT a FROM Apu a WHERE a.vMaterialesAPU BETWEEN :min AND :max")
    List<Apu> findByVMaterialesAPUBetween(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    @Query("SELECT a FROM Apu a WHERE a.vManoDeObraAPU BETWEEN :min AND :max")
    List<Apu> findByVManoDeObraAPUBetween(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    // Búsqueda por unidad
    List<Apu> findByUnidadesAPU(String unidad);

}