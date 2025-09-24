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
    private Apu crearAPUDesdeCSV(String[] record, Usuario usuario) {
        try {
            Apu apu = new Apu();
            apu.setIdUsuario(usuario); // Associate APU with current user

            // Map CSV columns to APU entity fields
            // CSV format: id_Apu,nombreAPU,descAPU,unidades,vMaterialesAPU,vManoDeObraAPU,vTransporteAPU,vMiscAPU
            apu.setNombreAPU(record[1].replace("\"", "").trim());     // nombreAPU (column 1)
            apu.setDescAPU(record[2].replace("\"", "").trim());       // descAPU (column 2)
            apu.setUnidadesAPU(record[3].replace("\"", "").trim());   // unidades (column 3)

            // Handle optional numeric values (columns 4-7)
            apu.setVMaterialesAPU(parseBigDecimal(record[4]));    // vMaterialesAPU
            apu.setVManoDeObraAPU(parseBigDecimal(record[5]));    // vManoDeObraAPU
            apu.setVTransporteAPU(parseBigDecimal(record[6]));    // vTransporteAPU
            apu.setVMiscAPU(parseBigDecimal(record[7]));          // vMiscAPU

            return apu;
        } catch (Exception e) {
            // Log error but continue processing other rows
            System.err.println("Error processing CSV record: " + String.join(",", record));
            return null;
        }
    }

    // Utility method to safely parse decimal values
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return BigDecimal.ZERO; // Default to zero if empty
        }
        try {
            String cleanValue = value.replace("\"", "").replace(",", "").trim();
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO; // Default to zero if parsing fails
        }
    }


}