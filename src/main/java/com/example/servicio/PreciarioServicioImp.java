package com.example.servicio;

import com.example.dao.PreciarioDao;
import com.example.dao.PreciarioApuDao;
import com.example.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PreciarioServicioImp implements PreciarioServicio {

    @Autowired
    private PreciarioDao preciarioDao;

    @Autowired
    private PreciarioApuDao preciarioApuDao;

    @Autowired
    private APUServicio apuServicio;

    @Override
    @Transactional(readOnly = true)
    public List<Preciario> listarTodos() {
        return preciarioDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Preciario> listarActivos() {
        return preciarioDao.listarActivos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Preciario> buscarPorNombre(String nombre) {
        return preciarioDao.buscarPorNombre(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public Preciario obtenerPorId(Long id) {
        return preciarioDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Preciario guardar(Preciario preciario) {
        return preciarioDao.save(preciario);
    }

    @Override
    @Transactional
    public void eliminar(Preciario preciario) {
        preciarioDao.delete(preciario);
    }

    @Override
    @Transactional
    public void agregarApu(Long idPreciario, Long idApu, Integer orden) {
        Preciario preciario = preciarioDao.findById(idPreciario).orElse(null);
        Apu apu = apuServicio.obtenerPorId(idApu);
        if (preciario != null && apu != null) {
            PreciarioApu pa = new PreciarioApu(preciario, apu, orden);
            preciarioApuDao.save(pa);
        }
    }

    @Override
    @Transactional
    public void eliminarApu(Long idPreciario, Long idApu) {
        PreciarioApuId id = new PreciarioApuId(idPreciario, idApu);
        preciarioApuDao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Apu> obtenerApusDePreciario(Long idPreciario) {
        return preciarioApuDao.findByPreciarioId(idPreciario).stream()
                .map(PreciarioApu::getApu)
                .toList();
    }
}