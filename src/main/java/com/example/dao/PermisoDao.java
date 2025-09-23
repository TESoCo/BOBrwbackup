package com.example.dao;

import com.example.domain.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoDao extends JpaRepository<Permiso, Long> {
    List<Permiso> findByNombrePermiso(String nombre);
    //

}
