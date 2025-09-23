package com.example.servicio;

import com.example.dao.APUDao;
import com.example.dao.AvanceDao;
import com.example.dao.FotoDatoDao;
import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FotoDatoServicioImp implements FotoDatoServicio{

    @Autowired
    private AvanceDao avanceDao;

    @Autowired
    private APUDao mateDao;

    @Autowired
    private FotoDatoDao fotoDatoDao;

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> listaFotoDatoAv (Avance avance) {
        return (List<FotoDato>) fotoDatoDao.findByIdAvance(avance);
    }

    @Override
    @Transactional
    public void salvar(FotoDato fotoDato) {
        fotoDatoDao.save(fotoDato);
    }

    @Override
    @Transactional
    public void borrar(FotoDato fotoDato) {
        fotoDatoDao.delete(fotoDato);
    }


    @Override
    @Transactional(readOnly = true)

    public FotoDato localizarFotoDato(Long entryId) {
        return fotoDatoDao.findById(entryId).orElse(null);
    }

    // Implementation of new search methods
    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdAvance(Long idAvance) {
        List<Avance> avances =avanceDao.findByIdAvance(idAvance);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdUsuario(Long idUsuario) {
        List<Avance> avances = avanceDao.findByIdUsuario_IdUsuario(idUsuario);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdObra(Long idObra) {
        List<Avance> avances = avanceDao.findByIdObra_IdObra(idObra);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdFotoDato(Long idFotoDato){
        return fotoDatoDao.findByIdFotoDato(idFotoDato);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorIdApu(Long idApu) {
        List<Avance> avances = avanceDao.findByIdApu_IdAPU(idApu);
        List<FotoDato> result = new ArrayList<>();
        if(avances!= null && !avances.isEmpty())
        {
            for(Avance avance: avances)
            {
                result.addAll(fotoDatoDao.findByIdAvance(avance));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorFecha(LocalDate fecha) {
        return fotoDatoDao.findByFechaFoto(fecha);
    }



    @Override
    @Transactional(readOnly = true)
    public List<FotoDato> buscarPorUsuarioYFecha(Usuario idUsuario, LocalDate fecha) {
        return fotoDatoDao.findByIdAvance_IdUsuarioAndFechaFoto(idUsuario,fecha);
    }


    public List<Apu> listarMateriales() {
        return (List<Apu>) mateDao.findAll();
    }

}
