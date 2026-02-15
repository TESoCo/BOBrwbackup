package com.example.servicio;

import com.example.dao.EquipoDao;
import com.example.domain.Equipo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EquipoServicioImpl implements EquipoServicio {

    @Autowired
    private EquipoDao equipoDao;

    @Override
    public List<Equipo> listarEquipos() {
        return equipoDao.findAll();
    }

    @Override
    public Equipo encontrarPorId(Long id) {
        return equipoDao.findById(id).orElse(null);
    }

    @Override
    public void guardar(Equipo equipo) {
        equipoDao.save(equipo);
    }

    @Override
    public void borrar(Equipo equipo) {
        equipoDao.delete(equipo);
    }

    @Override
    public List<Equipo> buscarPorDescripcion(String descripcion) {
        return equipoDao.findByDescEquipoContainingIgnoreCase(descripcion);
    }

    @Override
    public List<Equipo> buscarEquiposSinUsuarios() {
        return equipoDao.findByUsuariosCount(0);
    }

    @Override
    public List<Equipo> buscarEquiposSinProyectos() {
        return equipoDao.findByProyectosCount(0);
    }
}