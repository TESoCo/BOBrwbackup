package com.example.servicio;

import com.example.dao.APUDao;
import com.example.dao.MaterialDao;
import com.example.domain.Apu;
import com.example.domain.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialServicioImp implements MaterialServicio {

    @Autowired
    private MaterialDao materialDao;

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

}
