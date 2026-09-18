package com.example.dao;

import com.example.domain.Preciario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreciarioDao extends JpaRepository<Preciario, Long> {
    @Query("SELECT p FROM Preciario p WHERE p.activo = true AND LOWER(p.nombrePreciario) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Preciario> buscarPorNombre(@Param("nombre") String nombre);

    @Query("SELECT p FROM Preciario p WHERE p.activo = true")
    List<Preciario> listarActivos();

    @Query("SELECT p FROM Preciario p WHERE p.idUsuario.idUsuario = :idUsuario AND p.activo = true")
    List<Preciario> listarPorUsuario(@Param("idUsuario") Long idUsuario);
}