// ApusObraDao.java
package com.example.dao;

import com.example.domain.ApusObra;
import com.example.domain.ApusObraId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ApusObraDao extends JpaRepository<ApusObra, ApusObraId> {

        List<ApusObra> findByObra_IdObra(Long idObra);

        Optional<ApusObra> findByObra_IdObraAndApu_IdAPU(Long idObra, Long idAPU);
        boolean existsByObra_IdObraAndApu_IdAPU(Long idObra, Long idAPU);

        @Query("SELECT ao FROM ApusObra ao WHERE ao.apu.idAPU = :idAPU")
        List<ApusObra> findByApuId(@Param("idAPU") Long idAPU);

        @Modifying
        @Transactional
        void deleteByObra_IdObra(Long idObra);

        @Modifying
        @Transactional
        void deleteByApu_IdAPU(Long idAPU);

        // Métodos con cantidad
        List<ApusObra> findByCantidadGreaterThan(Double cantidad);
        List<ApusObra> findByCantidadBetween(Double min, Double max);

    }
