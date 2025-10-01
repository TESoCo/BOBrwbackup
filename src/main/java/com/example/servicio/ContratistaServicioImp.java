package com.example.servicio;

import com.example.dao.*;
import com.example.domain.Avance;
import com.example.domain.Contratista;
import com.example.domain.Inventario;
import com.example.domain.Obra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ContratistaServicioImp implements ContratistaServicio {

    @Autowired
    private AvanceDao avanceDao;

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private ObraDao obraDao;

    @Autowired
    private ContratistaDao contratistaDao;


    @Override
    @Transactional(readOnly = true)
    public List<Contratista> listarContratistas() {
        return (List<Contratista>) contratistaDao.findAll();
    }

    @Override
    @Transactional
    public void guardar(Contratista contratista) {
        contratistaDao.save(contratista);
    }

    @Override
    @Transactional
    public void borrar(Contratista contratista) {
        contratistaDao.delete(contratista);
    }

    @Override
    @Transactional(readOnly = true)
    public Contratista encontrarPorId(Long id) {
        return contratistaDao.findById(id).orElse(null);
    }

    // Métodos de búsqueda implementados
    @Override
    @Transactional(readOnly = true)
    public Contratista encontrarPorNombreContratista(String nombreContratista) {
        return contratistaDao.findByNombreContratista(nombreContratista);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contratista> buscarPorNombreObra(String nombreObra) {
        Obra obra = obraDao.findByNombreObra(nombreObra).get(0);
        List<Contratista> contratistas = new ArrayList<>();
        List<Avance> avances = avanceDao.findByIdObra(obra);
        if (avances != null && !avances.isEmpty()) {
            for (Avance avance : avances) {
                contratistas.add(avance.getIdContratista());
            }
        }
        ;
        return contratistas;
    }
}