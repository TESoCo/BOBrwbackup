package com.example.dao;

import com.example.domain.Proveedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorDao extends JpaRepository<Proveedor, Long> {

        boolean existsByInformacionComercial_NitRut(String nitRut);
        boolean existsByInformacionComercial_NitRutAndIdProveedorNot(String nitRut, Long idProveedor);

        // Útil si quieres cargar por NIT
        Proveedor findByInformacionComercial_NitRut(String nitRut);

        // Búsqueda por persona
        List<Proveedor> findByIdPersona_NombreContainingIgnoreCase(String nombre);
        List<Proveedor> findByIdPersona_CorreoContainingIgnoreCase(String correo);

        // Búsqueda por información comercial
        List<Proveedor> findByInformacionComercial_Banco(String banco);
        List<Proveedor> findByInformacionComercial_FormaPago(String formaPago);
        List<Proveedor> findByInformacionComercial_ProductoContainingIgnoreCase(String producto);

        // Búsqueda por material
        @Query("SELECT p FROM Proveedor p JOIN p.materialList m WHERE m.idMaterial = :idMaterial")
        List<Proveedor> findByMaterialId(@Param("idMaterial") Long idMaterial);

        @Query("SELECT p FROM Proveedor p JOIN p.materialList m WHERE m.nombreMaterial LIKE %:nombre%")
        List<Proveedor> findByMaterialNombreContaining(@Param("nombre") String nombre);

        // --- Para reportes: traer relaciones LAZY de una vez ---
        // Opción A: EntityGraph (sencillo y portable)
        //
        @EntityGraph(attributePaths = {"idPersona", "informacionComercial"})
        @Override
        List<Proveedor> findAll();

        // Si prefieres separar
        @EntityGraph(attributePaths = {"idPersona","informacionComercial","materialList"})
        @Query("select p from Proveedor p")
        List<Proveedor> findAllWithRels();

        // Opción B (alternativa): JOIN FETCH (si no usas EntityGraph)
        // OJO: si materialList es @OneToMany, usa DISTINCT para evitar duplicados
        @Query("SELECT DISTINCT p FROM Proveedor p " +
                "LEFT JOIN FETCH p.idPersona " +
                "LEFT JOIN FETCH p.informacionComercial " +
                "WHERE p.idProveedor = :id")
        Optional<Proveedor> findByIdWithDetails(@Param("id") Long id);

        // Específico con materiales (si se necesita)
        @EntityGraph(attributePaths = {"idPersona", "informacionComercial", "materialList"})
        @Query("SELECT DISTINCT p FROM Proveedor p LEFT JOIN FETCH p.materialList")
        List<Proveedor> findAllWithMaterials();




}