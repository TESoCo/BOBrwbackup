package com.example.dao;


import com.example.domain.Persona;
import org.springframework.data.repository.CrudRepository;

public interface PersonaDao extends CrudRepository<Persona, Long> {

    Persona findByIdPersona(Long idPersona );


}
