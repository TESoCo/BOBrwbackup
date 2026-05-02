package com.example.servicio;

import com.example.domain.Material;
import com.example.domain.PrecioMaterial;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


public interface MaterialServicio {

    List<Material> listarTodos();
    Material obtenerPorId(Long idMaterial);
    void guardar(Material material);
    void eliminar(Long idMaterial);
    Material obtenerPorNombre(String nombreMaterial);
    BigDecimal getPrecioActual(Long materialId);
    BigDecimal getPrecioPorProveedor(Long materialId, Long proveedorId);
    PrecioMaterial asignarPrecioAProveedor(Long materialId, Long proveedorId,
                                           BigDecimal precio, BigDecimal precioMinimoCompra);
    List<PrecioMaterial> getPreciosPorMaterial(Long materialId);
    List<Material> listarTodosConPrecios();

}
