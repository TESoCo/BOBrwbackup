package com.example.servicio;

import com.example.dao.ApuDao;
import com.example.domain.Apu;
import com.example.domain.Usuario;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class APUServicioImp implements APUServicio {

    @Autowired
    private ApuDao APUDao;

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

        try {
                CSVParser parser = new CSVParserBuilder()
                        .withSeparator(',')
                        .withQuoteChar('"')
                        .withEscapeChar('\\')
                        .build();


                CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                        .withCSVParser(parser)
                        .build();
                List<String[]> records = reader.readAll();
            reader.close();


            System.out.println("Total de filas en CSV: " + records.size()); // DEBUG

            if (!records.isEmpty()) {
                System.out.println("Encabezados: " + String.join(" | ", records.get(0)));
            }

            // Skip header row (index 0) and process data rows
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i); // Use .get() for List
                if (record.length >= 4) {
                    Apu apu = crearAPUDesdeCSV(record, usuario);
                    if (apu != null) {

                        if (apu.getDescAPU()!=null && apu.getDescAPU().length()>250)
                        {
                            String descripcionTruncada = apu.getDescAPU().substring(0, 250);
                            apu.setDescAPU(descripcionTruncada);
                            System.out.println("⚠️  Descripción truncada a 250 caracteres");
                        }
                        if (apu.getNombreAPU()!=null && apu.getNombreAPU().length()>100)
                        {
                            String nombreTruncado = apu.getNombreAPU().substring(0, 100);
                            apu.setNombreAPU(nombreTruncado);
                            System.out.println("⚠️  Nombre truncado a 100 caracteres");
                        }


                        apusImportados.add(apu);
                        System.out.println("APU agregado: " + apu.getNombreAPU()); // DEBUG
                    }
                }else {
                    System.err.println("Fila " + i + " ignorada - muy pocas columnas: " + record.length);
                }


            }
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV file", e);
        } catch (Exception e) {
            throw new IOException("Error processing CSV: " + e.getMessage(), e);
        }
        System.out.println("📊 Total de APUs procesados: " + apusImportados.size());
        return apusImportados;
    }

    public Apu crearAPUDesdeCSV(String[] record, Usuario usuario) {
        try {
            Apu apu = new Apu();
            apu.setIdUsuario(usuario);

            // Map CSV columns to APU fields
            // CSV format: id_Apu,nombreAPU,descAPU,unidades,vMaterialesAPU,vManoDeObraAPU,vTransporteAPU,vMiscAPU
            apu.setNombreAPU(record.length > 1 ? cleanValue(record[1]) : "");     // nombreAPU (column 1)
            apu.setDescAPU(record.length > 2 ? cleanValue(record[2]) : "");       // descAPU (column 2)
            apu.setUnidadesAPU(record.length > 3 ? cleanValue(record[3]) : "");   // unidades (column 3)

            // Handle optional numeric values (columns 4-7)
            apu.setVMaterialesAPU(record.length > 4 ? parseBigDecimal(record[4]) : BigDecimal.ZERO);    // vMaterialesAPU
            apu.setVManoDeObraAPU(record.length > 5 ? parseBigDecimal(record[5]) : BigDecimal.ZERO);    // vManoDeObraAPU
            apu.setVTransporteAPU(record.length > 6 ? parseBigDecimal(record[6]) : BigDecimal.ZERO);    // vTransporteAPU
            apu.setVMiscAPU(record.length > 7 ? parseBigDecimal(record[7]) : BigDecimal.ZERO);          // vMiscAPU

            return apu;
        } catch (Exception e) {
            System.err.println("Error processing CSV record: " + String.join(",", record));
            e.printStackTrace();
            return null;
        }
    }

    private String cleanValue(String value) {
        if (value == null) return "";
        try {
            // Limpiar caracteres problemáticos
            String cleaned = value
                    .replace("\"", "") // Remover comillas dobles
                    .replace("\uFFFD", "") // Remover caracteres de reemplazo Unicode
                    .replaceAll("[\\x00-\\x1F\\x7F]", "") // Remover caracteres de control
                    .replaceAll("\\s+", " ") // Normalizar espacios múltiples
                    .trim();

            // Verificar si hay caracteres problemáticos después de la limpieza
            if (cleaned.chars().anyMatch(c -> c > 0x7F && c != 0xA0)) {
                System.out.println("⚠️  Texto con caracteres especiales detectado: " + cleaned.substring(0, Math.min(50, cleaned.length())));
            }

            return cleaned.isEmpty() ? "" : cleaned;
        } catch (Exception e) {
            System.err.println("Error cleaning value: '" + value + "' - " + e.getMessage());
            return "";
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return BigDecimal.ZERO;
        }

        try { String cleanValue = value.replace("\"", "")
                    .replace(",", "") // Remover separadores de miles
                    .replace(" ", "")
                    .trim();

            // Si está vacío después de limpiar, retornar cero
            if (cleanValue.isEmpty()) {
                return BigDecimal.ZERO;
                }

            return new BigDecimal(cleanValue);

        } catch (NumberFormatException e) {
            System.err.println("Error parsing number: " + value);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public void guardarTodos(List<Apu> apus) {
        APUDao.saveAll(apus);
    }

    //calcular valor de APU
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPrecioTotalAPU(Apu apu) {
        BigDecimal total = BigDecimal.ZERO;

        if (apu.getVMaterialesAPU() != null) {
            total = total.add(apu.getVMaterialesAPU());
        }
        if (apu.getVManoDeObraAPU() != null) {
            total = total.add(apu.getVManoDeObraAPU());
        }
        if (apu.getVMiscAPU() != null) {
            total = total.add(apu.getVMiscAPU());
        }
        if (apu.getVTransporteAPU() != null) {
            total = total.add(apu.getVTransporteAPU());
        }

        return total;
    }


}

