package com.example.dao;

import com.example.domain.Avance;
import com.example.domain.Obra;
import com.example.domain.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvanceDao extends CrudRepository<Avance, Integer> {

    // Find by exact ID matches
    List<Avance> findByIdAvance(Integer idAvance);
    List<Avance> findByIdUsuario_IdUsuario(Integer idUsuario);
    List<Avance> findByIdObra_IdObra(Integer idObra);
    List<Avance> findByIdObra(Obra obra);
    List<Avance> findByApu_IdAPU(Integer idMatriz);

    // Find by fecha (exact and partial match)
    List<Avance> findByFechaAvance(LocalDate fechaAvance);

    List<Avance> findByFechaAvanceBetween(LocalDate start, LocalDate end);

    // Combined queries using existing fields
    List<Avance> findByIdUsuarioAndFechaAvance(Usuario idUsuario, LocalDate fechaAvance);



}
