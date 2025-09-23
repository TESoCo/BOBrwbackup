package com.example.dao;

import com.example.domain.Obra;
import com.example.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolDao extends JpaRepository<Rol, Long> {

    List<Rol> findByNombreRolIgnoreCase(String nombreRol);

}
