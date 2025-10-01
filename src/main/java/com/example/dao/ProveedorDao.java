package com.example.dao;

import com.example.domain.Proveedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProveedorDao extends JpaRepository<Proveedor, Long> {
        boolean existsByInformacionComercial_NitRut(String nitRut);
        boolean existsByInformacionComercial_NitRutAndIdProveedorNot(String nitRut, Long idProveedor);

        // Útil si quieres cargar por NIT
        Proveedor findByInformacionComercial_NitRut(String nitRut);

        // --- Para reportes: traer relaciones LAZY de una vez ---
        // Opción A: EntityGraph (sencillo y portable)
        @EntityGraph(attributePaths = {
                "idPersona",
                "informacionComercial",
                "materialList" // quítalo si no lo usas en reportes
        })
        List<Proveedor> findAll(); // si quieres, puedes dejar este como findAll con graph

        // Si prefieres separar el método:
        @EntityGraph(attributePaths = {"idPersona","informacionComercial","materialList"})
        @Query("select p from Proveedor p")
        List<Proveedor> findAllWithRels();

        // Opción B (alternativa): JOIN FETCH (si no usas EntityGraph)
        // OJO: si materialList es @OneToMany, usa DISTINCT para evitar duplicados
        @Query("""
           select distinct p
           from Proveedor p
           left join fetch p.idPersona per
           left join fetch p.informacionComercial ic
           left join fetch p.materialList m
           """)
        List<Proveedor> findAllJoinFetch();
}