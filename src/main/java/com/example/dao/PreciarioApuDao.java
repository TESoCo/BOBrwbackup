package com.example.dao;

import com.example.domain.PreciarioApu;
import com.example.domain.PreciarioApuId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreciarioApuDao extends JpaRepository<PreciarioApu, PreciarioApuId> {
    @Query("SELECT pa FROM PreciarioApu pa WHERE pa.preciario.idPreciario = :idPreciario ORDER BY pa.orden")
    List<PreciarioApu> findByPreciarioId(@Param("idPreciario") Long idPreciario);

    @Query("SELECT pa FROM PreciarioApu pa WHERE pa.apu.idAPU = :idApu")
    List<PreciarioApu> findByApuId(@Param("idApu") Long idApu);
}