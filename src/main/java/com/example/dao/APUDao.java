package com.example.dao;

import com.example.domain.Apu;
import org.springframework.data.repository.CrudRepository;
import java.util.List;


public interface APUDao extends CrudRepository<Apu, Integer> {

    List<Apu> findByNombreAPUContainingIgnoreCase(String nombreAPU);

}