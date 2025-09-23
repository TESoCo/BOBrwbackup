// ApusObraDao.java
package com.example.dao;

import com.example.domain.ApusObra;
import com.example.domain.ApusObraId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApusObraDao extends CrudRepository<ApusObra, ApusObraId> {


        @Query("SELECT ao FROM ApusObra ao WHERE ao.obra.idObra = :idObra")
        List<ApusObra> findByObraId(@Param("idObra") Long idObra);

        @Query("SELECT ao FROM ApusObra ao WHERE ao.apu.idAPU = :idAPU")
        List<ApusObra> findByApuId(@Param("idAPU") Long idAPU);

        @Query("DELETE FROM ApusObra ao WHERE ao.obra.idObra = :idObra")
        void deleteByObraId(@Param("idObra") Long idObra);
    }
