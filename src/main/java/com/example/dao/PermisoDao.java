package com.example.dao;

import com.example.domain.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermisoDao extends JpaRepository<Permiso, Long> {
    List<Permiso> findByNombrePermiso(String nombre);
    List<Permiso> findByNombrePermisoContainingIgnoreCase(String nombre);
    Optional<Permiso> findOneByNombrePermiso(String nombre);

    // Búsqueda por rol
    @Query("SELECT p FROM Permiso p JOIN p.rolList r WHERE r.idRol = :idRol")
    List<Permiso> findByRolId(@Param("idRol") Long idRol);

    @Query("SELECT p FROM Permiso p JOIN p.rolList r WHERE r.nombreRol = :nombreRol")
    List<Permiso> findByRolNombre(@Param("nombreRol") String nombreRol);

    // Búsqueda de permisos no asignados a un rol
    @Query("SELECT p FROM Permiso p WHERE p NOT IN (SELECT p2 FROM Permiso p2 JOIN p2.rolList r WHERE r.idRol = :idRol)")
    List<Permiso> findPermisosNotInRol(@Param("idRol") Long idRol);

    // Verificación
    boolean existsByNombrePermiso(String nombrePermiso);

    // Conteo
    Long countByNombrePermisoContaining(String nombre);

    // Ordenamiento
    List<Permiso> findAllByOrderByNombrePermisoAsc();

    // Búsqueda por múltiples nombres
    List<Permiso> findByNombrePermisoIn(List<String> nombres);

}
