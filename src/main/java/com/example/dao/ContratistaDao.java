package com.example.dao;

import com.example.domain.Contratista;
import com.example.domain.Material;
import org.springframework.data.repository.CrudRepository;

public interface ContratistaDao extends CrudRepository<Contratista, Long> {
        Contratista findByNombreContratista (String nombreContratista);


}
