package com.example.dao;

import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FotoDatoDao extends JpaRepository<FotoDato, Long> {

    // Find by exact ID matches
    List<FotoDato> findByIdAvance(Avance idAvance);
    List<FotoDato> findByIdFotoDato(Long idFotoDato);
    List<FotoDato> findByIdAvance_IdUsuario(Usuario idUsuario);
    List<FotoDato> findByIdAvance_IdApu(Apu idApu);

    // Búsqueda por avance
    List<FotoDato> findByIdAvance_IdUsuario_IdUsuario(Long idUsuario);
    List<FotoDato> findByIdAvance_IdApu_IdAPU(Long idApu);

    // Find by fecha (exact and partial match)
    List<FotoDato> findByFechaFoto(LocalDate fechaFoto);
    List<FotoDato> findByFechaFotoBetween(LocalDate start, LocalDate end);

    // Búsqueda por ubicación
    List<FotoDato> findByCooNFotoAndCooEFoto(Double cooN, Double cooE);
    List<FotoDato> findByCooNFotoBetween(Double minN, Double maxN);
    List<FotoDato> findByCooEFotoBetween(Double minE, Double maxE);

    // Combined queries using existing fields
    List<FotoDato> findByIdAvance_IdUsuarioAndFechaFoto(Usuario idUsuario, LocalDate fechaAvance);

    // Consultas complejas
    @Query("SELECT fd FROM FotoDato fd WHERE fd.idAvance.idObra.idObra = :idObra")
    List<FotoDato> findByObraId(@Param("idObra") Long idObra);

    @Query("SELECT fd FROM FotoDato fd WHERE fd.idAvance.idContratista.idContratista = :idContratista")
    List<FotoDato> findByContratistaId(@Param("idContratista") Long idContratista);

}
