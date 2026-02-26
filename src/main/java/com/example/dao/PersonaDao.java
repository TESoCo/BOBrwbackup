package com.example.dao;


import com.example.domain.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonaDao extends JpaRepository<Persona, Long> {

    Persona findByIdPersona(Long idPersona );
    // Búsqueda por nombre
    List<Persona> findByNombreContainingIgnoreCase(String nombre);
    List<Persona> findByNombreStartingWithIgnoreCase(String nombre);
    List<Persona> findByApellidoContainingIgnoreCase(String apellido);
    List<Persona> findByNombreAndApellido(String nombre, String apellido);

    // Búsqueda por contacto
    List<Persona> findByCorreo(String correo);
    List<Persona> findByCorreoContainingIgnoreCase(String correo);
    List<Persona> findByTelefono(String telefono);
    List<Persona> findByTelefonoContaining(String telefono);

    // Búsqueda combinada
    List<Persona> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(String nombre, String apellido);
    List<Persona> findByCorreoContainingIgnoreCaseOrTelefonoContaining(String correo, String telefono);

    // Verificación
    boolean existsByCorreo(String correo);
    boolean existsByTelefono(String telefono);

    // Consultas complejas
    @Query("SELECT p FROM Persona p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Persona> buscarPorNombreOApellido(@Param("texto") String texto);

    @Query("SELECT p FROM Persona p WHERE p.correo LIKE %:dominio%")
    List<Persona> findByCorreoDominio(@Param("dominio") String dominio);

    // Búsqueda por usuario
    @Query("SELECT p FROM Persona p JOIN Usuario u ON p.idPersona = u.persona.idPersona WHERE u.idUsuario = :idUsuario")
    Optional<Persona> findByUsuarioId(@Param("idUsuario") Long idUsuario);

    // Búsqueda por proveedor
    @Query("SELECT p FROM Persona p JOIN Proveedor pr ON p.idPersona = pr.idPersona.idPersona WHERE pr.idProveedor = :idProveedor")
    Optional<Persona> findByProveedorId(@Param("idProveedor") Long idProveedor);

    // Búsqueda por contratista
    @Query("SELECT p FROM Persona p JOIN Contratista c ON p.idPersona = c.idPersona.idPersona WHERE c.idContratista = :idContratista")
    Optional<Persona> findByContratistaId(@Param("idContratista") Long idContratista);

    // Ordenamiento
    List<Persona> findAllByOrderByApellidoAscNombreAsc();
    List<Persona> findByNombreContainingIgnoreCaseOrderByApellidoAsc(String nombre);

}
