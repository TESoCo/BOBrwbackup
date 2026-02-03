package com.example.dao;

import com.example.domain.Proyecto;
import com.example.domain.Equipo;
import com.example.domain.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProyectoDao extends JpaRepository<Proyecto, Long> {

    // Find by exact ID matches
    List<Proyecto> findByIdProyecto(Long idProyecto);

    // Find by description (exact and partial match)
    List<Proyecto> findByDescProyecto(String descProyecto);
    List<Proyecto> findByDescProyectoContaining(String descProyecto);
    List<Proyecto> findByDescProyectoContainingIgnoreCase(String descProyecto);

    // Find by equipo (assigned team)
    List<Proyecto> findByEquipo(Equipo equipo);
    List<Proyecto> findByEquipoIdEquipo(Long idEquipo);
    List<Proyecto> findByEquipoIsNull();

    // Find by obra (related works)
    List<Proyecto> findByObrasContaining(Obra obra);

    // Combined queries
    List<Proyecto> findByDescProyectoContainingAndEquipoIdEquipo(String descProyecto, Long idEquipo);

    // Find projects with no obras
    List<Proyecto> findByObrasIsEmpty();

    // Find projects with at least one obra
    List<Proyecto> findByObrasIsNotEmpty();

    // USING JPQL QUERIES FOR COMPLEX QUERIES

    // Find projects by obra count
    @Query("SELECT p FROM Proyecto p WHERE SIZE(p.obras) = :count")
    List<Proyecto> findByObrasCount(@Param("count") int count);

    @Query("SELECT p FROM Proyecto p WHERE SIZE(p.obras) > :count")
    List<Proyecto> findByObrasCountGreaterThan(@Param("count") int count);

    @Query("SELECT p FROM Proyecto p WHERE SIZE(p.obras) < :count")
    List<Proyecto> findByObrasCountLessThan(@Param("count") int count);

    @Query("SELECT p FROM Proyecto p WHERE SIZE(p.obras) BETWEEN :min AND :max")
    List<Proyecto> findByObrasCountBetween(@Param("min") int min, @Param("max") int max);

    // Note: The following methods require adding fields to the Proyecto entity first

    //TODO: IF you add these fields to Proyecto.java, uncomment these methods:
    // List<Proyecto> findByEstadoProyecto(String estadoProyecto);
    // List<Proyecto> findByFechaInicio(LocalDate fechaInicio);
    // List<Proyecto> findByFechaFin(LocalDate fechaFin);
    // List<Proyecto> findByFechaInicioBetween(LocalDate start, LocalDate end);
    // List<Proyecto> findByFechaFinBetween(LocalDate start, LocalDate end);
    // List<Proyecto> findByEstadoProyectoAndEquipoIdEquipo(String estadoProyecto, Long idEquipo);
    // List<Proyecto> findByPresupuestoBetween(Double minPresupuesto, Double maxPresupuesto);
    // List<Proyecto> findByClienteContainingIgnoreCase(String cliente);
    // List<Proyecto> findByUbicacionContainingIgnoreCase(String ubicacion);
    // List<Proyecto> findByGerenteProyectoId(Long idUsuario);
    // Long countByEstadoProyecto(String estadoProyecto);
    // Double sumPresupuestoByEstadoProyecto(String estadoProyecto);

}