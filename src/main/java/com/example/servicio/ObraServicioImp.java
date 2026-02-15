package com.example.servicio;

import com.example.dao.ApuDao;
import com.example.dao.ApusObraDao;
import com.example.dao.ObraDao;
import com.example.domain.Apu;
import com.example.domain.ApusObra;
import com.example.domain.Obra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObraServicioImp implements ObraServicio {

    @Autowired
    private ObraDao obraDao;

    @Autowired
    private ApuDao apuDao;

    @Autowired
    private ApusObraDao apusObraDao;

    @Autowired
    private APUServicio apuServicio;

    @Override
    @Transactional(readOnly = true)
    public List<Obra> listaObra() {
        return (List<Obra>) obraDao.findAll();
    }

    @Override
    @Transactional
    public void salvar(Obra obraGuardar) {
        obraDao.save(obraGuardar);
    }

    @Override
    @Transactional
    public void borrar(Obra obraBorrar) {
        obraDao.delete(obraBorrar);
    }

    @Override
    @Transactional
    public void actualizar(Obra obraActualizar) {
        obraDao.save(obraActualizar);
    }

    @Override
    @Transactional(readOnly = true)

    public Obra localizarObra(Long entryId) {
        return obraDao.findById(entryId).orElse(null);
    }

    public List<Obra>  findByObraName(String obraName) {
        return obraDao.findByNombreObra(obraName);
    }
    public List<Obra> findByObraNameContaining(String obraName) {
        return obraDao.findByNombreObraContaining(obraName);
    }
    public List<Obra> findByObraNameIgnoreCase(String obraName) {
        return obraDao.findByNombreObraIgnoreCase(obraName);
    }


    public List<Apu> listarApus() {
        return (List<Apu>) apuDao.findAll();
    }

    @Override
    @Transactional
    public void agregarApuAObra(Obra obra, Apu apu) {
        ApusObra apusObra = new ApusObra();
        apusObra.setObra(obra);
        apusObra.setApu(apu);
        apusObraDao.save(apusObra);
    }

    @Override
    @Transactional
    public void agregarApuAObraConCantidad(Obra obra, Apu apu, Double cantObra) {
        ApusObra apusObra = new ApusObra();
        apusObra.setObra(obra);
        apusObra.setApu(apu);
        apusObra.setCantidad(cantObra);
        apusObraDao.save(apusObra);
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> obtenerApusPorObra(Long idObra) {
        // This method should return a map of APU IDs to quantities
        // Since your current structure doesn't store quantities,
        // you'll need to modify the ApusObra entity to include quantity
        Map<Long, Double> result = new HashMap<>();

        // For now, returning empty map - you'll need to implement quantity storage
        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Apu> obtenerApusEntidadesPorObra(Long idObra) {
        List<ApusObra> apusObra = apusObraDao.findByObra_IdObra(idObra);
        return apusObra.stream()
                .map(ApusObra::getApu)
                .toList();
    }





}