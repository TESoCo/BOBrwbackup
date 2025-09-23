package com.example.servicio;

import com.example.domain.Apu;
import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;

import java.time.LocalDate;
import java.util.List;

public interface FotoDatoServicio {

    public List<FotoDato> listaFotoDatoAv (Avance avance);

    public void salvar(FotoDato fotoDato);

    public void borrar(FotoDato fotoDato);

    FotoDato localizarFotoDato (Long entryId);

    // New search methods
    List<FotoDato> buscarPorIdAvance(Long idAvance);
    List<FotoDato> buscarPorIdUsuario(Long idUsuario);
    List<FotoDato> buscarPorIdObra(Long idObra);
    List<FotoDato> buscarPorIdFotoDato(Long idFotoDato);
    List<FotoDato> buscarPorIdApu(Long idApu);

    List<FotoDato>  buscarPorFecha(LocalDate fecha);


    // Combined search methods
    List<FotoDato>  buscarPorUsuarioYFecha(Usuario id_usuario, LocalDate fecha);


}
