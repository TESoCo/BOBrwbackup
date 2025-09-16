package com.example.servicio;

import com.example.dao.APUDao;
import com.example.domain.Apu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class APUServicioImp implements APUServicio {

    @Autowired
    private APUDao APUDao;

    @Override
    @Transactional(readOnly = true)
    public List<Apu> listarElementos() {
        return (List<Apu>) APUDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Apu obtenerPorId(Integer id_apu) {
        return APUDao.findById(id_apu).orElse(null);
    }

    @Override
    @Transactional
    public void guardar(Apu nuevoApu) {
        APUDao.save(nuevoApu);
    }

    @Override
    @Transactional
    public void eliminar(Apu apuBorrar) {
        APUDao.delete(apuBorrar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Apu> buscarPorNombre(String nombre) {
        return APUDao.findByNombreAPUContainingIgnoreCase(nombre);
    }
}