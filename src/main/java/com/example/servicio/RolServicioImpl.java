package com.example.servicio;

import com.example.dao.RolDao;
import com.example.domain.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolServicioImpl implements RolServicio {

    @Autowired
    private RolDao rolDao;

    @Override
    public Rol buscarPorId(Long id) {
        return rolDao.findById(id).orElse(null);
    }

    @Override
    public void guardar(Rol rol) {
        rolDao.save(rol);
    }

    @Override
    public List<Rol> listarRoles() {
        return rolDao.findAll();
    }

    @Override
    public List<Rol> buscarPorNombre(String nombreRol) { return rolDao.findByNombreRolIgnoreCase(nombreRol); }

    @Override
    public void eliminar(Rol rol) {
        rolDao.delete(rol);
    }

}
