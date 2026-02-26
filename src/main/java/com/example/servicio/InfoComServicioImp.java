package com.example.servicio;

import com.example.dao.InformacionComercialDao;
import com.example.domain.InformacionComercial;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InfoComServicioImp implements InfoComServicio {

    @Autowired
    private InformacionComercialDao informacionComercialDao;

    @Override
    @Transactional(readOnly = true)
    public List<InformacionComercial> comercialList() {
        return (List<InformacionComercial>) informacionComercialDao.findAll();
    }

    @Override
    @Transactional
    public InformacionComercial salvar(InformacionComercial informacionComercial) {return informacionComercialDao.save(informacionComercial);}

    @Override
    @Transactional
    public void borrar(InformacionComercial informacionComercial) {informacionComercialDao.delete(informacionComercial);}

    @Override
    @Transactional(readOnly = true)
    public InformacionComercial localizarInformacionComercial(InformacionComercial informacionComercial) {
        return informacionComercialDao.findByIdInfoComerc(informacionComercial.getIdInfoComerc());
    }

    @Override
    @Transactional(readOnly = true)
    public InformacionComercial localizarPorId(Long id){
        return informacionComercialDao.findByIdInfoComerc(id);
    }

}
