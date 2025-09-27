package com.example.servicio;

import com.example.domain.Apu;
import com.example.domain.Usuario;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface APUServicio {

    List<Apu> listarElementos();

    Apu obtenerPorId(Long id_apu);

    void guardar(Apu nuevoApu);

    void eliminar(Apu apuBorrar);

    // Optional: If you need to search materials
    List<Apu> buscarPorNombre(String nombre);

    // Main method that orchestrates the import process
    public List<Apu> importarAPUsDesdeCSV(MultipartFile file, Usuario usuario)throws IOException;

    // Helper method to convert CSV row to APU object
    public Apu crearAPUDesdeCSV(String[] record, Usuario usuario);

    public void guardarTodos(List<Apu> apus);
}