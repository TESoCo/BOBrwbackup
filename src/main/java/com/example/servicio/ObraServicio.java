package com.example.servicio;

import com.example.domain.Apu;
import com.example.domain.Obra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ObraServicio {

    public List<Obra> listaObra();

    public void salvar(Obra obraGuardar);

    void actualizar(Obra obraActualizar);

    public void borrar(Obra obraBorrar);

    Obra localizarObra(Long entryId);

    List<Obra> findByObraName(String obraName);
    List<Obra> findByObraNameContaining(String obraName);
    List<Obra> findByObraNameIgnoreCase(String obraName);

    public List<Apu> listarApus();

    void agregarApuAObra(Obra obra, Apu apu);
    void agregarApuAObraConCantidad(Obra obra, Apu apu, Double cantObra);
    Map<Long, Double> obtenerApusPorObra(Long idObra);
    List<Apu> obtenerApusEntidadesPorObra(Long idObra);

}
