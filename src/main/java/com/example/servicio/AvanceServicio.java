package com.example.servicio;

import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.Usuario;

import java.time.LocalDate;
import java.util.List;

public interface AvanceServicio {

    public List<Avance> listaAvance();

    public void salvar(Avance avance);

    void actualizar(Avance avance);

    public void borrar(Avance avance);

    Avance localizarAvance(Long entryId);

    // New search methods
    List<Avance> buscarPorIdAvance(Long idAvance);
    List<Avance> buscarPorIdUsuario(Long idUsuario);
    List<Avance> buscarPorIdObra(Long idObra);
    List<Avance> buscarPorIdApu(Long idMatriz);

    List<Avance> buscarPorFecha(LocalDate fecha);




    // Combined search methods
    List<Avance> buscarPorUsuarioYFecha(Usuario id_usuario, String fecha);



    public List<Apu> listarMateriales();
}
