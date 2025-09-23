package com.example.servicio;

import com.example.domain.Contratista;
import com.example.domain.Usuario;

import java.util.List;

public interface ContratistaServicio {
    public List<Contratista> listarContratistas();
    public void guardar(Contratista contratista);
    public void borrar(Contratista contratista);
    public Contratista encontrarPorId(Long id);
    Contratista encontrarPorNombreContratista(String nombreContratista);
    public List<Contratista> buscarPorNombreObra(String nombreObra);

}