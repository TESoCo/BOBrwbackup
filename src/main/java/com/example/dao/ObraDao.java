package com.example.dao;

import com.example.domain.Obra;
import com.example.domain.Proyecto;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ObraDao extends JpaRepository<Obra, Long> {

    // Find budgets by exact work name match (case-sensitive)
    List<Obra> findByNombreObra(String nombreObra);

    // Find budgets by work name containing the given string (case-sensitive)
    List<Obra> findByNombreObraContaining(String nombreObra);

    // Find budgets by exact work name match (case-insensitive)
    List<Obra> findByNombreObraIgnoreCase(String nombreObra);

    // Find budgets by work name containing the given string (case-insensitive)
    List<Obra> findByNombreObraContainingIgnoreCase(String nombreObra);

    // Búsqueda por etapa
    List<Obra> findByEtapa(String etapa);
    List<Obra> findByEtapaContainingIgnoreCase(String etapa);

    // Búsqueda por usuario
    List<Obra> findByIdUsuario_IdUsuario(Long idUsuario);

    // Búsqueda por proyecto
    List<Obra> findByProyecto_IdProyecto(Long idProyecto);
    List<Obra> findByProyectoIsNull();

    // Búsqueda por fechas
    List<Obra> findByFechaIni(LocalDate fecha);
    List<Obra> findByFechaFinCalculada(LocalDate fecha);
    List<Obra> findByFechaIniBetween(LocalDate start, LocalDate end);
    List<Obra> findByFechaFinCalculadaBetween(LocalDate start, LocalDate end);

    // Búsqueda por ubicación
    List<Obra> findByCooNObraBetween(Double minN, Double maxN);
    List<Obra> findByCooEObraBetween(Double minE, Double maxE);

    // Búsqueda por anular
    List<Obra> findByAnular(boolean anular);

    // Búsqueda combinada
    List<Obra> findByIdUsuario_IdUsuarioAndEtapa(Long idUsuario, String etapa);
    List<Obra> findByProyecto_IdProyectoAndAnularFalse(Long idProyecto);

    // Consultas complejas
    @Query("SELECT o FROM Obra o WHERE o.fechaIni <= :today AND o.fechaFinManual >= :today")
    List<Obra> findObrasEnCurso(@Param("today") LocalDate today);

    @Query("SELECT o FROM Obra o WHERE o.fechaFinManual < :today")
    List<Obra> findObrasFinalizadas(@Param("today") LocalDate today);

    @Query("SELECT o FROM Obra o WHERE o.fechaIni > :today")
    List<Obra> findObrasPorIniciar(@Param("today") LocalDate today);

    // Consultas con APUs
    @Query("SELECT DISTINCT o FROM Obra o JOIN o.apusObraList ao WHERE ao.apu.idAPU = :idApu")
    List<Obra> findByApuId(@Param("idApu") Long idApu);

    // Consultas con avances
    @Query("SELECT DISTINCT o FROM Obra o JOIN Avance a ON o.idObra = a.idObra.idObra WHERE a.idContratista.idContratista = :idContratista")
    List<Obra> findByContratistaId(@Param("idContratista") Long idContratista);

    // Estadísticas
    @Query("SELECT COUNT(o) FROM Obra o WHERE o.etapa = :etapa AND o.anular = false")
    Long countByEtapaAndActive(@Param("etapa") String etapa);

    @Query("SELECT o.etapa, COUNT(o) FROM Obra o WHERE o.anular = false GROUP BY o.etapa")
    List<Object[]> countByEtapaGrouped();

    // Ordenamiento
    List<Obra> findAllByOrderByFechaIniDesc();
    List<Obra> findByProyecto_IdProyectoOrderByNombreObraAsc(Long idProyecto);

    //Métodos para el nuevo identificador global de obra
    // NUEVOS MÉTODOS PARA EL FLUJO DE ETAPAS
    List<Obra> findByIdentificadorUnico(String identificadorUnico);

    @Query("SELECT o FROM Obra o WHERE o.identificadorUnico = :identificador AND o.etapa = :etapa")
    Obra findByIdentificadorUnicoAndEtapa(@Param("identificador") String identificador,
                                          @Param("etapa") String etapa);

    @Query("SELECT o FROM Obra o WHERE o.identificadorUnico = :identificador AND o.anular = false")
    List<Obra> findActiveByIdentificadorUnico(@Param("identificador") String identificador);

    @Query("SELECT o FROM Obra o WHERE o.proyecto.idProyecto = :idProyecto AND o.etapa = :etapa")
    Obra findByProyectoIdAndEtapa(@Param("idProyecto") Long idProyecto,
                                  @Param("etapa") String etapa);

    @Query("SELECT o FROM Obra o WHERE o.identificadorUnico = :identificador ORDER BY o.idObra ASC")
    List<Obra> findByIdentificadorUnicoOrdered(@Param("identificador") String identificador);

    List<Obra> findByIdUsuario(Usuario usuario);


    List<Obra> findByProyecto(Proyecto proyecto);
    List<Obra> findByProyectoIn(List<Proyecto> proyectos);

}
