package com.example.servicio;

import com.example.dao.*;
import com.example.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProveedorServicioImp implements ProveedorServicio {

    @Autowired
    private ProveedorDao proveedorDao;

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private ObraDao obraDao;

    @Autowired
    private ContratistaDao contratistaDao;


    @Override
    @Transactional(readOnly = true)
    public List<Proveedor> listarProveedores()
        { return (List<Proveedor>) proveedorDao.findAll();
    }

    @Override
    @Transactional
    public void guardar(Proveedor proveedor) {
        proveedorDao.save(proveedor);
    }

    @Override
    @Transactional
    public void borrar(Proveedor proveedor) {
        proveedorDao.delete(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Proveedor encontrarPorId(Long id) {
        return proveedorDao.findById(id).orElse(null);
    }

    // Métodos de búsqueda implementados
    @Override
    @Transactional(readOnly = true)
    public Proveedor encontrarPorIdPersona(Persona idPersona) {
        return proveedorDao.findByIdPersona(idPersona);
    }


}