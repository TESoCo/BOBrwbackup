package com.example.servicio;

import com.example.dao.APUDao;
import com.example.dao.MaterialesAPUDao;
import com.example.domain.Apu;
import com.example.domain.MaterialesApu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialesAPUServicioImp implements MaterialesAPUServicio {

    @Autowired
    private MaterialesAPUDao materialesAPUDao;

    @Autowired
    private APUDao apuDao;



    @Override
    @Transactional(readOnly = true)
    public List<MaterialesApu> listarElementos(){
        return materialesAPUDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialesApu> obtenerPorId(Long id_apu) {
        return materialesAPUDao.findByApuIdApu(id_apu);
    }


    @Override
    @Transactional
    public void guardar(MaterialesApu materialesApu) {
        materialesAPUDao.save(materialesApu);
    }

    @Override
    @Transactional
    public void eliminar(MaterialesApu materialesApu) {
        materialesAPUDao.delete(materialesApu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialesApu> buscarPorNombre(String nombre) {
        Apu apu = apuDao.findByNombreAPUContainingIgnoreCase(nombre).get(0);
        return materialesAPUDao.findByApuIdApu(apu.getIdAPU());
    }
}