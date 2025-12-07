package com.example.servicio;

import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.domain.Usuario;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface FotoDatoServicio {

    public int cantidad ();

    public List<FotoDato> listaFotoDatoAv (Avance avance);

    void salvar(FotoDato fotoDato, MultipartFile archivo) throws IOException;

    public void borrar(FotoDato fotoDato);

    FotoDato localizarFotoDato (Long entryId);

    void salvarConBytes(FotoDato fotoDato, byte[] imageBytes, String filename, String contentType) throws IOException;

    public byte[] obtenerArchivoFoto(String gridfsFileId) throws IOException;

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
