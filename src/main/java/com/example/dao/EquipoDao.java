package com.example.dao;

import com.example.domain.Equipo;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoDao extends JpaRepository<Equipo, Long> {

    // Find by exact ID matches
    List<Equipo> findByIdEquipo(Long idEquipo);

    // Find by description (exact and partial match)
    Equipo findByDescEquipo(String descEquipo);
    List<Equipo> findByDescEquipoContaining(String descEquipo);
    List<Equipo> findByDescEquipoContainingIgnoreCase(String descEquipo);

    // Find by usuario (team members)
    List<Equipo> findByUsuariosContaining(Usuario usuario);

    // Custom query methods for specific business logic

    // Find teams with no projects
    List<Equipo> findByProyectosIsEmpty();

    // Find teams with at least one project
    List<Equipo> findByProyectosIsNotEmpty();

    // Find teams with no users
    List<Equipo> findByUsuariosIsEmpty();

    // Find teams with at least one user
    List<Equipo> findByUsuariosIsNotEmpty();

    // Find teams by project count - USING JPQL QUERIES
    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) = :size")
    List<Equipo> findByProyectosCount(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) > :size")
    List<Equipo> findByProyectosCountGreaterThan(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) >= :size")
    List<Equipo> findByProyectosCountGreaterThanEqual(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) < :size")
    List<Equipo> findByProyectosCountLessThan(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) <= :size")
    List<Equipo> findByProyectosCountLessThanEqual(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.proyectos) BETWEEN :min AND :max")
    List<Equipo> findByProyectosCountBetween(@Param("min") int min, @Param("max") int max);

    // Find teams by user count - USING JPQL QUERIES
    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) = :size")
    List<Equipo> findByUsuariosCount(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) > :size")
    List<Equipo> findByUsuariosCountGreaterThan(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) >= :size")
    List<Equipo> findByUsuariosCountGreaterThanEqual(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) < :size")
    List<Equipo> findByUsuariosCountLessThan(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) <= :size")
    List<Equipo> findByUsuariosCountLessThanEqual(@Param("size") int size);

    @Query("SELECT e FROM Equipo e WHERE SIZE(e.usuarios) BETWEEN :min AND :max")
    List<Equipo> findByUsuariosCountBetween(@Param("min") int min, @Param("max") int max);

    // Find teams by project ID
    @Query("SELECT e FROM Equipo e JOIN e.proyectos p WHERE p.idProyecto = :idProyecto")
    List<Equipo> findByProyectoId(@Param("idProyecto") Long idProyecto);

    // Find teams by user ID
    @Query("SELECT e FROM Equipo e JOIN e.usuarios u WHERE u.idUsuario = :idUsuario")
    List<Equipo> findByUsuarioId(@Param("idUsuario") Long idUsuario);
}