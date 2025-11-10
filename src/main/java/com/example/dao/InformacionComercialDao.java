package com.example.dao;

import com.example.domain.InformacionComercial;
import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformacionComercialDao extends JpaRepository <InformacionComercial, Long> {
    InformacionComercial findByNitRut(String nitRut);
    List<InformacionComercial> findByDireccion(String direccion);
    List <InformacionComercial> findByProducto(String producto);
    InformacionComercial findByidInfoComerc(Long idInfoComerc);


}
