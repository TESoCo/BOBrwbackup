package com.example.repository;

import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.Obra;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvanceRepository extends JpaRepository<Avance, Long> {

    // Custom query methods for Avance entity
    List<Avance> findByIdUsuario(Usuario idUsuario);

    List<Avance> findByIdObra(Obra idObra);

    List<Avance> findByIdApu(Apu apu);

    List<Avance> findByFechaAvance(LocalDate fechaAvance);

    List<Avance> findByIdUsuarioAndFechaAvance(Usuario idUsuario, LocalDate fechaAvance);

    @Query("SELECT a FROM Avance a WHERE a.idAvance = :idAvance")
    List<Avance> findByIdAvance(@Param("idAvance") Integer idAvance);
}