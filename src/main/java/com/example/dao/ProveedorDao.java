package com.example.dao;

import com.example.domain.Contratista;
import com.example.domain.Persona;
import com.example.domain.Proveedor;
import org.springframework.data.repository.CrudRepository;

public interface ProveedorDao extends CrudRepository<Proveedor, Long> {
        Proveedor findByIdPersona (Persona idPersona);


}
