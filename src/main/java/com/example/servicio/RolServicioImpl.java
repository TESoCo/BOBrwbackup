package com.example.servicio;

import com.example.dao.RolDao;
import com.example.domain.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolServicioImpl implements RolServicio {

    @Autowired
    private RolDao rolDao;

    @Override
    public Rol buscarPorId(Integer id) {
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
}
