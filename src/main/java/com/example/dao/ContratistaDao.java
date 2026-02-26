package com.example.dao;

import com.example.domain.Contratista;
import com.example.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContratistaDao extends JpaRepository<Contratista, Long> {
        Contratista findByNombreContratista (String nombreContratista);
        List<Contratista> findByNombreContratistaContainingIgnoreCase(String nombreContratista);

        // Búsqueda por NIT/RUT
        List<Contratista> findByInformacionComercial_NitRut(String nitRut);
        List<Contratista> findByInformacionComercial_NitRutContaining(String nitRut);

        // Búsqueda por persona
        List<Contratista> findByIdPersona_IdPersona(Long idPersona);
        List<Contratista> findByIdPersona_NombreContainingIgnoreCase(String nombre);

        // Búsqueda por información comercial
        List<Contratista> findByInformacionComercial_Banco(String banco);
        List<Contratista> findByInformacionComercial_FormaPago(String formaPago);

        // Verificación
        boolean existsByInformacionComercial_NitRut(String nitRut);
        boolean existsByNombreContratista(String nombre);

        // Conteo
        Long countByInformacionComercial_Banco(String banco);

}
