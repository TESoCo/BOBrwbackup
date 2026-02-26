package com.example.dao;

import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioDao extends JpaRepository <Usuario, Long> {
    List<Usuario> findByNombreUsuarioContainingIgnoreCase(String username);
    Usuario findByNombreUsuario(String username);

    List<Usuario> findByRol_NombreRol(String nomRol);
    List<Usuario> findByRol_NombreRolContainingIgnoreCase(String nomRol);
    List<Usuario> findByRol_IdRol(Long idRol);

    List<Usuario> findByPersona_NombreContainingIgnoreCase(String nombre);
    List<Usuario> findByPersona_ApellidoContainingIgnoreCase(String apellido);
    List<Usuario> findByPersona_Correo(String correo);
    List<Usuario> findByPersona_Telefono(String telefono);

    List<Usuario> findByEquipo_IdEquipo(Long idEquipo);
    List<Usuario> findByEquipo_DescEquipoContainingIgnoreCase(String descEquipo);
    List<Usuario> findByEquipoIsNull();

    List<Usuario> findByCargo(String cargo);
    List<Usuario> findByCargoContainingIgnoreCase(String cargo);

    List<Usuario> findByRol_NombreRolAndCargo(String rol, String cargo);
    List<Usuario> findByPersona_NombreContainingIgnoreCaseAndEquipo_IdEquipo(String nombre, Long idEquipo);

    // Búsqueda por texto en nombre de usuario o persona
    @Query("SELECT u FROM Usuario u WHERE " +
            "u.nombreUsuario LIKE %:texto% OR " +
            "u.persona.nombre LIKE %:texto% OR " +
            "u.persona.apellido LIKE %:texto%")
    List<Usuario> buscarPorTexto(@Param("texto") String texto);

    // Consultas complejas
    @Query("SELECT u FROM Usuario u WHERE u.idUsuario IN (" +
            "SELECT a.idUsuario.idUsuario FROM Avance a WHERE a.idObra.idObra = :idObra)")
    List<Usuario> findUsuariosConAvancesEnObra(@Param("idObra") Long idObra);

    @Query("SELECT u FROM Usuario u WHERE u.idUsuario IN (" +
            "SELECT i.idUsuario.idUsuario FROM Inventario i WHERE i.idObra.idObra = :idObra)")
    List<Usuario> findUsuariosConInventariosEnObra(@Param("idObra") Long idObra);

    // Verificación
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByPersona_Correo(String correo);

    // Conteo
    Long countByRol_NombreRol(String nombreRol);
    Long countByEquipo_IdEquipo(Long idEquipo);

    // Estadísticas
    @Query("SELECT u.cargo, COUNT(u) FROM Usuario u GROUP BY u.cargo")
    List<Object[]> countByCargo();

    @Query("SELECT u.equipo.descEquipo, COUNT(u) FROM Usuario u WHERE u.equipo IS NOT NULL GROUP BY u.equipo.idEquipo, u.equipo.descEquipo")
    List<Object[]> countByEquipo();

    // Ordenamiento
    List<Usuario> findAllByOrderByPersona_ApellidoAscPersona_NombreAsc();
    List<Usuario> findByRol_NombreRolOrderByPersona_ApellidoAsc(String nombreRol);

    // Búsqueda por múltiples roles
    @Query("SELECT u FROM Usuario u WHERE u.rol.nombreRol IN :roles")
    List<Usuario> findByRoles(@Param("roles") List<String> roles);


}
