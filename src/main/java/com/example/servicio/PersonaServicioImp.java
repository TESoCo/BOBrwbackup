package com.example.servicio;


import com.example.dao.PersonaDao;
import com.example.domain.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonaServicioImp implements PersonaServicio {

    @Autowired
    private PersonaDao personaDao;

    @Override
    @Transactional(readOnly = true)
    public List<Persona> listaPersonas() {
        return (List<Persona>) personaDao.findAll();
    }

    @Override
    @Transactional
    public Persona salvar(Persona persona) {return personaDao.save(persona);}

    @Override
    @Transactional
    public void borrar(Persona persona) {personaDao.delete(persona);}

    @Override
    @Transactional(readOnly = true)
    public Persona localizarPersona(Persona persona) {
        return personaDao.findByIdPersona(persona.getIdPersona());
    }
}
