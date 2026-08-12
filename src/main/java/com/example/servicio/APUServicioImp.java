package com.example.servicio;

import com.example.dao.ApuDao;
import com.example.dao.MaterialesApuDao;
import com.example.dao.PrecioMaterialDao;
import com.example.domain.Apu;
import com.example.domain.MaterialesApu;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class APUServicioImp implements APUServicio {

    @Autowired
    private ApuDao APUDao;

    @Autowired
    private MaterialesApuDao materialesApuDao;

    @Autowired
    private PrecioMaterialDao precioMaterialDao;

    @Autowired
    private MaterialServicio materialServicio;

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



    // Calcular costo de materiales de un APU con proveedor específico
    @Transactional(readOnly = true)
    public BigDecimal calcularCostoMaterialesConProveedor(Long apuId, Long proveedorId) {
        List<MaterialesApu> materialesApu = materialesApuDao.findByApu_IdAPU(apuId);

        if (materialesApu == null || materialesApu.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return materialesApu.stream()
                .map(ma -> {
                    BigDecimal precioUnitario = materialServicio.getPrecioPorProveedor(
                            ma.getMaterial().getIdMaterial(), proveedorId);

                    if (precioUnitario == null) {
                        precioUnitario = materialServicio.getPrecioActual(ma.getMaterial().getIdMaterial());
                    }

                    BigDecimal cantidad = BigDecimal.valueOf(ma.getCantidad());
                    return precioUnitario.multiply(cantidad);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //Actualizar vMaterialesAPU con el mejor precio disponible
    @Transactional
    public void actualizarCostoMateriales(Long apuId) {
        Apu apu = obtenerPorId(apuId);
        if (apu != null) {
            // Usar el mejor precio disponible (más bajo entre todos los proveedores)
            BigDecimal costoTotal = calcularMejorCostoMateriales(apuId);
            apu.setVMaterialesAPU(costoTotal);
            APUDao.save(apu);
        }
    }

    // Calcular el mejor costo posible (más bajo entre proveedores)
    @Transactional(readOnly = true)
    public BigDecimal calcularMejorCostoMateriales(Long apuId) {
        List<MaterialesApu> materialesApu = materialesApuDao.findByApu_IdAPU(apuId);

        if (materialesApu == null || materialesApu.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return materialesApu.stream()
                .map(ma -> {
                    BigDecimal mejorPrecio = materialServicio.getPrecioActual(ma.getMaterial().getIdMaterial());
                    BigDecimal cantidad = BigDecimal.valueOf(ma.getCantidad());
                    return mejorPrecio.multiply(cantidad);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //calcular valor de APU
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPrecioTotalAPU(Apu apu) {
        BigDecimal total = BigDecimal.ZERO;

        // Si el APU tiene materiales, calcular dinámicamente
        if (apu.getMaterialesApus() != null && !apu.getMaterialesApus().isEmpty()) {
            total = calcularMejorCostoMateriales(apu.getIdAPU());
        } else {
            // Fallback a valores guardados
            if (apu.getVMaterialesAPU() != null) {
                total = total.add(apu.getVMaterialesAPU());
            }
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



    // CSV Import Implementation - FIXED VERSION
    @Override
    @Transactional
    public List<Apu> importarAPUsDesdeCSV(MultipartFile file, Usuario usuario) throws IOException {
        List<Apu> apusImportados = new ArrayList<>();

        try {
                // Detectar y eliminar BOM al inicio del archivo
                InputStream inputStream = file.getInputStream();
                byte[] bom = new byte[3];
                inputStream.mark(3);
                inputStream.read(bom);

                // Si tiene BOM UTF-8, saltarlo
                if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                    System.out.println("⚠️ BOM UTF-8 detectado, saltando...");
                    inputStream.reset();
                    inputStream.skip(3);
                } else {
                    inputStream.reset();
                }

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

                // Limpiar cada campo
                for (int j = 0; j < record.length; j++) {
                    record[j] = cleanValue(record[j]);
                }

                // Verificar que los campos obligatorios no estén vacíos
                String nombre = record.length > 1 ? record[1] : "";
                String descripcion = record.length > 2 ? record[2] : "";
                String unidad = record.length > 3 ? record[3] : "";

                if (nombre.isEmpty() || descripcion.isEmpty() || unidad.isEmpty()) {
                    System.err.println("Fila " + i + " ignorada - campos obligatorios vacíos");
                    continue;
                }

                Apu apu = crearAPUDesdeCSV(record, usuario);
                if (apu != null) {
                    apusImportados.add(apu);
                    System.out.println("✅ APU agregado: " + apu.getNombreAPU());
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
            // CSV format: item,nombreAPU,descAPU,unidades,duracion, vMaterialesAPU,vManoDeObraAPU,vTransporteAPU,vMiscAPU

            String nombre = record.length > 1 ? cleanValue(record[1]) : "";// nombreAPU (column 1)
            String descripcion = record.length > 2 ? cleanValue(record[2]) : "";// descAPU (column 2)
            String unidad = record.length > 3 ? cleanValue(record[3]) : "";// unidades (column 3)
            String duracionStr = record.length > 4 ? cleanValue(record[4]) : "";// duracion (column 4)
            String vMaterialesStr = record.length > 5 ? cleanValue(record[5]) : "";// vMaterialesAPU
            String vManoObraStr = record.length > 6 ? cleanValue(record[6]) : "";// vManoDeObraAPU
            String vTransporteStr = record.length > 7 ? cleanValue(record[7]) : "";// vTransporteAPU
            String vMiscStr = record.length > 8 ? cleanValue(record[8]) : "";// vMiscAPU


            // VALIDAR Y TRUNCAR NOMBRE (máximo 255 caracteres)
            if (nombre.isEmpty()) {
                System.err.println("⚠️ Nombre vacío, usando valor por defecto");
                nombre = "APU SIN NOMBRE";
            }

            if (nombre.length() > 255) {
                nombre = nombre.substring(0, 255);
                System.out.println("⚠️ Nombre truncado a 255 caracteres");
            }
            apu.setNombreAPU(nombre);

            // VALIDAR DESCRIPCIÓN (TEXT no tiene límite estricto, pero limitamos a 1000 para seguridad)
            if (descripcion.isEmpty()) {
                descripcion = "SIN DESCRIPCIÓN";
            }

            if (descripcion.length() > 1000) {
                descripcion = descripcion.substring(0, 1000);
                System.out.println("⚠️ Descripción truncada a 1000 caracteres");
            }
            apu.setDescAPU(descripcion);

            // VALIDAR UNIDAD (máximo 50 caracteres, no puede estar vacía)
            if (unidad == null || unidad.trim().isEmpty()) {
                unidad = "N/A"; // Valor por defecto si está vacío
                System.out.println("⚠️ Unidad vacía, usando 'N/A'");
            } else if (unidad.length() > 50) {
                unidad = unidad.substring(0, 50);
                System.out.println("⚠️ Unidad truncada a 50 caracteres");
            }
            apu.setUnidadesAPU(unidad);

            // Parsear duración (si es null o vacío, usar 1.0)
            BigDecimal duracion = parseBigDecimal(duracionStr);
            if (duracion == null || duracion.compareTo(BigDecimal.ZERO) <= 0) {
                duracion = new BigDecimal("1.0");
            }
            apu.setDuracionAPU(duracion);

            // Parsear valores monetarios
            apu.setVMaterialesAPU(parseBigDecimalSafe(vMaterialesStr));
            apu.setVManoDeObraAPU(parseBigDecimalSafe(vManoObraStr));
            apu.setVTransporteAPU(parseBigDecimalSafe(vTransporteStr));
            apu.setVMiscAPU(parseBigDecimalSafe(vMiscStr));

            // Establecer activo por defecto
            apu.setActivo(true);

            // Recalcular el total ANTES de guardar
            apu.recalcularTotal();

            // DEBUG: Mostrar qué se está procesando
            System.out.println("📝 APU procesado: " + apu.getNombreAPU() +
                    " | Unidad: " + apu.getUnidadesAPU() +
                    " | Total: " + apu.getVTotalApu());


            return apu;

        } catch (Exception e) {
            System.err.println("Error processing CSV record: " + String.join(",", record));
            e.printStackTrace();
            return null;
        }
    }

    // Método seguro para parsear BigDecimal
    private BigDecimal parseBigDecimalSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty() || value.equals("null")) {
                return BigDecimal.ZERO;
            }
            String clean = value.trim().replace(",", "").replace("$", "");
            if (clean.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Error parseando número: '" + value + "' - usando 0");
            return BigDecimal.ZERO;
        }
    }

    private String cleanValue(String value) {
        if (value == null) return "";
        try {
            // Limpiar caracteres problemáticos
            String cleaned = value
                    .replace("\uFEFF", "")      // Eliminar BOM
                    .replace("\"", "") // Remover comillas dobles
                    .replace("'", "")            // Remover comillas simples
                    .replace("\uFFFD", "") // Remover caracteres de reemplazo Unicode
                    .replaceAll("[\\x00-\\x1F\\x7F]", "") // Remover caracteres de control
                    .replaceAll("\\s+", " ") // Normalizar espacios múltiples
                    .trim();

            // Verificar si hay caracteres problemáticos después de la limpieza
            if (cleaned.chars().anyMatch(c -> c > 0x7F && c != 0xA0)) {
                System.out.println("⚠️  Texto con caracteres especiales detectado: " + cleaned.substring(0, Math.min(50, cleaned.length())));
            }

            // Si quedó vacío después de limpiar, retornar cadena vacía
            if (cleaned.isEmpty()) {
                return "";
            }

            // Verificar caracteres especiales y reemplazar si es necesario
            cleaned = cleaned.replaceAll("[^\\p{ASCII}]", " ").trim();

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
        if (apus == null || apus.isEmpty()) {
            System.out.println("⚠️ Lista de APUs vacía, nada que guardar");
            return;
        }

        System.out.println("📊 Intentando guardar " + apus.size() + " APUs");

        // Filtrar y validar APUs antes de guardar
        List<Apu> apusValidos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (int i = 0; i < apus.size(); i++) {
            Apu apu = apus.get(i);
            try {
                // Validar campos obligatorios
                if (apu.getIdUsuario() == null) {
                    errores.add("APU #" + (i+1) + ": Usuario no asignado");
                    continue;
                }

                if (apu.getNombreAPU() == null || apu.getNombreAPU().trim().isEmpty()) {
                    errores.add("APU #" + (i+1) + ": Nombre vacío o nulo");
                    continue;
                }

                if (apu.getDescAPU() == null || apu.getDescAPU().trim().isEmpty()) {
                    errores.add("APU #" + (i+1) + ": Descripción vacía o nula");
                    continue;
                }

                if (apu.getUnidadesAPU() == null || apu.getUnidadesAPU().trim().isEmpty()) {
                    apu.setUnidadesAPU("N/A");
                    System.out.println("⚠️ APU #" + (i+1) + " unidad vacía, usando 'N/A'");
                }

                // Asegurar valores por defecto
                if (apu.getVMaterialesAPU() == null) apu.setVMaterialesAPU(BigDecimal.ZERO);
                if (apu.getVManoDeObraAPU() == null) apu.setVManoDeObraAPU(BigDecimal.ZERO);
                if (apu.getVTransporteAPU() == null) apu.setVTransporteAPU(BigDecimal.ZERO);
                if (apu.getVMiscAPU() == null) apu.setVMiscAPU(BigDecimal.ZERO);
                if (apu.getActivo() == null) apu.setActivo(true);

                // Recalcular total
                apu.recalcularTotal();

                // Truncar nombres largos si es necesario
                if (apu.getNombreAPU().length() > 255) {
                    apu.setNombreAPU(apu.getNombreAPU().substring(0, 255));
                }

                apusValidos.add(apu);

            } catch (Exception e) {
                errores.add("APU #" + (i+1) + " (" + apu.getNombreAPU() + "): " + e.getMessage());
            }
        }

        // Mostrar errores de validación
        if (!errores.isEmpty()) {
            System.err.println("❌ ERRORES DE VALIDACIÓN:");
            for (String error : errores) {
                System.err.println("   - " + error);
            }
        }

        // Guardar APUs válidos
        if (!apusValidos.isEmpty()) {
            try {
                APUDao.saveAll(apusValidos);
                System.out.println("✅ " + apusValidos.size() + " APUs guardados exitosamente");
            } catch (Exception e) {
                System.err.println("❌ Error al guardar APUs en lote: " + e.getMessage());
                e.printStackTrace();

                // Intentar guardar uno por uno para identificar el problema
                System.out.println("🔄 Intentando guardar uno por uno...");
                int guardados = 0;
                for (Apu apu : apusValidos) {
                    try {
                        APUDao.save(apu);
                        guardados++;
                    } catch (Exception ex) {
                        System.err.println("❌ Error guardando APU '" + apu.getNombreAPU() + "': " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                System.out.println("✅ APUs guardados individualmente: " + guardados + " de " + apusValidos.size());
                throw new RuntimeException("Error al guardar APUs: " + e.getMessage(), e);
            }
        } else {
            throw new RuntimeException("No hay APUs válidos para guardar");
        }
    }




}

