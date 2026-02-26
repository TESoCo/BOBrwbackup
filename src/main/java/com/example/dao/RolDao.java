package com.example.dao;

import com.example.domain.Obra;
import com.example.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolDao extends JpaRepository<Rol, Long> {

    // Búsqueda por nombre
    List<Rol> findByNombreRolIgnoreCase(String nombreRol);
    Optional<Rol> findByNombreRol(String nombreRol); // AGREGADO
    List<Rol> findByNombreRolContainingIgnoreCase(String nombreRol);

    // Búsqueda por descripción
    List<Rol> findByDescripRolContainingIgnoreCase(String descripcion);

    // Búsqueda por permiso
    @Query("SELECT r FROM Rol r JOIN r.permisoList p WHERE p.idPermiso = :idPermiso")
    List<Rol> findByPermisoId(@Param("idPermiso") Long idPermiso);

    @Query("SELECT r FROM Rol r JOIN r.permisoList p WHERE p.nombrePermiso = :nombrePermiso")
    List<Rol> findByPermisoNombre(@Param("nombrePermiso") String nombrePermiso);

    // Búsqueda por usuario
    @Query("SELECT r FROM Rol r JOIN r.usuarios u WHERE u.idUsuario = :idUsuario")
    Optional<Rol> findByUsuarioId(@Param("idUsuario") Long idUsuario);

    // Búsqueda de roles con cierta cantidad de permisos
    @Query("SELECT r FROM Rol r WHERE SIZE(r.permisoList) >= :minPermisos")
    List<Rol> findByMinPermisos(@Param("minPermisos") int minPermisos);

    @Query("SELECT r FROM Rol r WHERE SIZE(r.permisoList) BETWEEN :min AND :max")
    List<Rol> findByPermisosCountBetween(@Param("min") int min, @Param("max") int max);

    // Búsqueda de roles sin usuarios
    @Query("SELECT r FROM Rol r WHERE SIZE(r.usuarios) = 0")
    List<Rol> findRolesSinUsuarios();

    // Verificación
    boolean existsByNombreRol(String nombreRol);

    // Conteo
    Long countByNombreRolContaining(String nombre);

    // Estadísticas
    @Query("SELECT r.nombreRol, COUNT(u) FROM Rol r LEFT JOIN r.usuarios u GROUP BY r.idRol, r.nombreRol ORDER BY COUNT(u) DESC")
    List<Object[]> countUsuariosByRol();

    // Ordenamiento
    List<Rol> findAllByOrderByNombreRolAsc();

    // Búsqueda por múltiples nombres
    List<Rol> findByNombreRolIn(List<String> nombres);
}

