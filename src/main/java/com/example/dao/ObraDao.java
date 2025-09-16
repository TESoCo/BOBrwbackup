package com.example.dao;

import com.example.domain.Obra;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface ObraDao extends CrudRepository<Obra, Integer> {

    // Find budgets by exact work name match (case-sensitive)
    List<Obra> findByNombreObra(String nombreObra);

    // Find budgets by work name containing the given string (case-sensitive)
    List<Obra> findByNombreObraContaining(String nombreObra);

    // Find budgets by exact work name match (case-insensitive)
    List<Obra> findByNombreObraIgnoreCase(String nombreObra);

    // Find budgets by work name containing the given string (case-insensitive)
    List<Obra> findByNombreObraContainingIgnoreCase(String nombreObra);



}
