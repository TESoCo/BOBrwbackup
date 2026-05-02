package com.example.servicio;

import com.example.dao.MaterialDao;
import com.example.dao.PrecioMaterialDao;
import com.example.domain.Material;
import com.example.domain.PrecioMaterial;
import com.example.domain.Proveedor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialServicioImp implements MaterialServicio {

    @Autowired
    private MaterialDao materialDao;

    @Autowired
    private PrecioMaterialDao precioMaterialDao;

    @Autowired
    private ProveedorServicio proveedorServicio;

    @Override
    @Transactional(readOnly = true)
    public List<Material> listarTodos() {
        return (List<Material>) materialDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Material obtenerPorId(Long idMaterial) {
        return materialDao.findById(idMaterial).orElse(null);
    }

    @Override
    @Transactional
    public void guardar(Material material) {
        materialDao.save(material);
    }

    @Override
    @Transactional
    public void eliminar(Long idMaterial) {
        materialDao.deleteById(idMaterial);
    }

    @Override
    @Transactional(readOnly = true)
    public Material obtenerPorNombre(String nombreMaterial) {
        return materialDao.findByNombreMaterial(nombreMaterial) .orElse(null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPrecioActual(Long materialId) {
        return precioMaterialDao.findMejorPrecioByMaterialId(materialId)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPrecioPorProveedor(Long materialId, Long proveedorId) {
        return precioMaterialDao
                .findByMaterial_IdMaterialAndProveedor_IdProveedorAndActivoTrue(materialId, proveedorId)
                .map(PrecioMaterial::getPrecioUnitario)
                .orElse(null);
    }

    @Transactional
    public PrecioMaterial asignarPrecioAProveedor(Long materialId, Long proveedorId,
                                                  BigDecimal precio, BigDecimal precioMinimoCompra) {
        Material material = obtenerPorId(materialId);
        Proveedor proveedor = proveedorServicio.buscarPorId(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no existe"));

        // Desactivar precios anteriores del mismo proveedor
        precioMaterialDao.findByMaterial_IdMaterialAndProveedor_IdProveedorAndActivoTrue(materialId, proveedorId)
                .ifPresent(p -> {
                    p.setActivo(false);
                    p.setFechaVigenciaHasta(LocalDateTime.now());
                    precioMaterialDao.save(p);
                });

        // Crear nuevo precio
        PrecioMaterial nuevoPrecio = new PrecioMaterial();
        nuevoPrecio.setMaterial(material);
        nuevoPrecio.setProveedor(proveedor);
        nuevoPrecio.setPrecioUnitario(precio);
        nuevoPrecio.setPrecioMinimoCompra(precioMinimoCompra);
        nuevoPrecio.setFechaVigenciaDesde(LocalDateTime.now());
        nuevoPrecio.setActivo(true);

        return precioMaterialDao.save(nuevoPrecio);
    }

    // Obtener todos los precios de un material
    @Transactional(readOnly = true)
    public List<PrecioMaterial> getPreciosPorMaterial(Long materialId) {
        return precioMaterialDao.findByMaterial_IdMaterialAndActivoTrue(materialId);
    }

    // Obtener todos los materiales con sus precios (para el frontend)
    @Transactional(readOnly = true)
    public List<Material> listarTodosConPrecios() {
        List<Material> materiales = listarTodos();
        // Inicializar la colección de precios para evitar LazyInitializationException
        materiales.forEach(m -> m.getPreciosPorProveedor().size());
        return materiales;
    }

}
