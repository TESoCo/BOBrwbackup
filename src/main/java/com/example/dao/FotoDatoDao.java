package com.example.dao;

import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface FotoDatoDao extends CrudRepository<FotoDato, Long> {

    // Find by exact ID matches
    List<FotoDato> findByIdAvance(Avance idAvance);
    List<FotoDato> findByIdFotoDato(Long idFotoDato);
    List<FotoDato> findByIdAvance_IdUsuario(Usuario idUsuario);
    List<FotoDato> findByIdAvance_IdApu(Apu idApu);

    // Find by fecha (exact and partial match)
    List<FotoDato> findByFechaFoto(LocalDate fechaFoto);

    List<FotoDato> findByFechaFotoBetween(LocalDate start, LocalDate end);

    // Combined queries using existing fields
    List<FotoDato> findByIdAvance_IdUsuarioAndFechaFoto(Usuario idUsuario, LocalDate fechaAvance);



}
