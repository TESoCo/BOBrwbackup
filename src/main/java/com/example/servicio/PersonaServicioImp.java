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
    public Persona salvar(Persona persona) {

        // Validar y asignar valores por defecto para evitar violaciones de constraint
        if (persona.getNombre() == null || persona.getNombre().trim().isEmpty()) {
            persona.setNombre("Usuario");
        }
        if (persona.getApellido() == null || persona.getApellido().trim().isEmpty()) {
            persona.setApellido("Sin Apellido");
        }
        if (persona.getTelefono() == null || persona.getTelefono().trim().isEmpty()) {
            persona.setTelefono("0000000000");
        }
        if (persona.getCorreo() == null || persona.getCorreo().trim().isEmpty()) {
            persona.setCorreo("usuario@" + System.currentTimeMillis() + ".tmp");
        }

        return personaDao.save(persona);

    }

    @Override
    @Transactional
    public void borrar(Persona persona) {personaDao.delete(persona);}

    @Override
    @Transactional(readOnly = true)
    public Persona localizarPersona(Persona persona) {
        return personaDao.findByIdPersona(persona.getIdPersona());
    }

}
