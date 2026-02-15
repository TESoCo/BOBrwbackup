package com.example.dao;

import com.example.domain.Avance;
import com.example.domain.Obra;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvanceDao extends JpaRepository<Avance, Long> {

    // Find by exact ID matches
    List<Avance> findByIdAvance(Long idAvance);
    List<Avance> findByIdUsuario_IdUsuario(Long idUsuario);
    List<Avance> findByIdObra_IdObra(Long idObra);
    List<Avance> findByIdObra(Obra obra);
    List<Avance> findByIdApu_IdAPU(Long idApu);
    List<Avance> findByIdContratista_IdContratista(Long idContratista);

    // Find by fecha (exact and partial match)
    List<Avance> findByFechaAvance(LocalDate fechaAvance);
    List<Avance> findByFechaAvanceBetween(LocalDate start, LocalDate end);
    List<Avance> findByFechaAvanceBefore(LocalDate date);
    List<Avance> findByFechaAvanceAfter(LocalDate date);

    // Búsqueda por cantidad ejecutada
    List<Avance> findByCantEjecGreaterThan(Double cantidad);
    List<Avance> findByCantEjecBetween(Double min, Double max);

    // Combined queries using existing fields
    List<Avance> findByIdUsuarioAndFechaAvance(Usuario idUsuario, LocalDate fechaAvance);

    // Búsqueda combinada
    List<Avance> findByIdUsuario_IdUsuarioAndFechaAvance(Long idUsuario, LocalDate fechaAvance);
    List<Avance> findByIdObra_IdObraAndIdContratista_IdContratista(Long idObra, Long idContratista);

    // Búsqueda por anular
    List<Avance> findByAnular(boolean anular);

}
