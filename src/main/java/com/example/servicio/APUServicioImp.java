package com.example.servicio;

import com.example.dao.APUDao;
import com.example.domain.Apu;
import com.example.domain.Usuario;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class APUServicioImp implements APUServicio {

    @Autowired
    private APUDao APUDao;

    @Override
    @Transactional(readOnly = true)
    public List<Apu> listarElementos() {
        return (List<Apu>) APUDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Apu obtenerPorId(Long id_apu) {
        return APUDao.findById(id_apu).orElse(null);
    }

    @Override
    @Transactional
    public void guardar(Apu nuevoApu) {
        APUDao.save(nuevoApu);
    }

    @Override
    @Transactional
    public void eliminar(Apu apuBorrar) {
        APUDao.delete(apuBorrar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Apu> buscarPorNombre(String nombre) {
        return APUDao.findByNombreAPUContainingIgnoreCase(nombre);
    }

    // CSV Import Implementation - FIXED VERSION
    @Override
    public List<Apu> importarAPUsDesdeCSV(MultipartFile file, Usuario usuario) throws IOException {
        List<Apu> apusImportados = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();

            // Skip header row (index 0) and process data rows
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i); // Use .get() for List
                if (record.length >= 7) {
                    Apu apu = crearAPUDesdeCSV(record, usuario);
                    if (apu != null) {
                        apusImportados.add(apu);
                    }
                }
            }
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV file", e);
        }

        return apusImportados;
    }

    private Apu crearAPUDesdeCSV(String[] record, Usuario usuario) {
        try {
            Apu apu = new Apu();
            apu.setIdUsuario(usuario);

            // Map CSV columns to APU fields
            // CSV format: id_Apu,nombreAPU,descAPU,unidades,vMaterialesAPU,vManoDeObraAPU,vTransporteAPU,vMiscAPU
            apu.setNombreAPU(cleanValue(record[1]));     // nombreAPU (column 1)
            apu.setDescAPU(cleanValue(record[2]));       // descAPU (column 2)
            apu.setUnidadesAPU(cleanValue(record[3]));   // unidades (column 3)

            // Handle optional numeric values (columns 4-7)
            apu.setVMaterialesAPU(parseBigDecimal(record[4]));    // vMaterialesAPU
            apu.setVManoDeObraAPU(parseBigDecimal(record[5]));    // vManoDeObraAPU
            apu.setVTransporteAPU(parseBigDecimal(record[6]));    // vTransporteAPU
            apu.setVMiscAPU(parseBigDecimal(record[7]));          // vMiscAPU

            return apu;
        } catch (Exception e) {
            System.err.println("Error processing CSV record: " + String.join(",", record));
            e.printStackTrace();
            return null;
        }
    }

    private String cleanValue(String value) {
        if (value == null) return "";
        return value.replace("\"", "").trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return BigDecimal.ZERO;
        }
        try {
            String cleanValue = value.replace("\"", "").replace(",", "").trim();
            return cleanValue.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number: " + value);
            return BigDecimal.ZERO;
        }
    }
}

