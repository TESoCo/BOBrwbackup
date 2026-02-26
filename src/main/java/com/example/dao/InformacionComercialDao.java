package com.example.dao;

import com.example.domain.InformacionComercial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformacionComercialDao extends JpaRepository <InformacionComercial, Long> {
    InformacionComercial findByNitRut(String nitRut);
    List<InformacionComercial> findByNitRutContaining(String nitRut);
    List<InformacionComercial> findByDireccion(String direccion);
    List<InformacionComercial> findByDireccionContainingIgnoreCase(String direccion);
    List <InformacionComercial> findByProducto(String producto);
    List<InformacionComercial> findByProductoContainingIgnoreCase(String producto);
    InformacionComercial findByIdInfoComerc(Long idInfoComerc);
    List<InformacionComercial> findByBanco(String banco);
    List<InformacionComercial> findByFormaPago(String formaPago);
    List<InformacionComercial> findByBancoAndFormaPago(String banco, String formaPago);
    List<InformacionComercial> findByCorreoElectronicoContainingIgnoreCase(String correo);
    List<InformacionComercial> findByNumCuenta(String numCuenta);
    List<InformacionComercial> findByNumCuentaContaining(String numCuenta);
    boolean existsByNitRut(String nitRut);
    boolean existsByNumCuenta(String numCuenta);
    Long countByBanco(String banco);
    Long countByFormaPago(String formaPago);
    List<InformacionComercial> findByProductoAndBanco(String producto, String banco);

}
