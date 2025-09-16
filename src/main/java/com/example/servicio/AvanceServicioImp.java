package com.example.servicio;

import com.example.dao.AvanceDao;
import com.example.dao.APUDao;
import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AvanceServicioImp implements AvanceServicio{

    @Autowired
    private AvanceDao avanceDao;

    @Autowired
    private APUDao mateDao;

    @Override
    @Transactional(readOnly = true)
    public List<Avance> listaAvance() {
        return (List<Avance>) avanceDao.findAll();
    }

    @Override
    @Transactional
    public void salvar(Avance avance) {
        avanceDao.save(avance);
    }

    @Override
    @Transactional
    public void borrar(Avance avance) {
        avanceDao.delete(avance);
    }

    @Override
    @Transactional
    public void actualizar(Avance avance) {
        avanceDao.save(avance);
    }

    @Override
    @Transactional(readOnly = true)

    public Avance localizarAvance(Integer entryId) {
        return avanceDao.findById(entryId).orElse(null);
    }

    // Implementation of new search methods
    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorIdAvance(Integer idAvance) {
        return avanceDao.findByIdAvance(idAvance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorIdUsuario(Integer idUsuario) {
        return avanceDao.findByIdUsuario_IdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorIdObra(Integer idObra) {
        return avanceDao.findByIdObra_IdObra(idObra);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorIdMatriz(Integer idMatriz) {
        return avanceDao.findByApu_IdAPU(idMatriz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorFecha(LocalDate fecha) {
        return avanceDao.findByFechaAvance(fecha);
    }



    @Override
    @Transactional(readOnly = true)
    public List<Avance> buscarPorUsuarioYFecha(Usuario idUsuario, String fecha) {
        return avanceDao.findByIdUsuarioAndFechaAvance(idUsuario, LocalDate.parse(fecha));
    }


    public List<Apu> listarMateriales() {
        return (List<Apu>) mateDao.findAll();
    }

}
