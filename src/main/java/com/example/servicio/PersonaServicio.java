package com.example.servicio;

import com.example.domain.Persona;

import java.util.List;

public interface PersonaServicio {

    public List<Persona> listaPersonas();

    public Persona salvar(Persona persona);

    public void borrar(Persona persona);

    public Persona localizarPersona(Persona persona);
}
