package com.example.servicio;

import com.example.domain.Proveedor;
import java.util.List;
import java.util.Optional;

public interface ProveedorServicio {
    List<Proveedor> listar();
    Optional<Proveedor> buscarPorId(Long id);
    Proveedor guardar(Proveedor proveedor);   // crea/actualiza
    void eliminar(Long id);


}
