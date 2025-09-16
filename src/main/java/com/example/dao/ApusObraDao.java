// ApusObraDao.java
package com.example.dao;

import com.example.domain.ApusObra;
import com.example.domain.ApusObraId;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface ApusObraDao extends CrudRepository<ApusObra, ApusObraId> {
    List<ApusObra> findByObraIdObra(Integer idObra);
    List<ApusObra> findByApuIdAPU(Integer idAPU);
    void deleteByObraIdObra(Integer idObra);
}