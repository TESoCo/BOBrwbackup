package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/avances")

public class ControladorAvance
{
    //Servicios para utilizar
    @Autowired
    private AvanceServicio avanceServicio;
    @Autowired
    private ObraServicio obraServicio;
    @Autowired
    private APUServicio apuServicio;
    @Autowired
    private UsuarioServicio usuarioServicio;
    @Autowired
    private ContratistaServicio contratistaServicio;
    @Autowired
    private ProveedorServicio proveedorServicio;
    @Autowired
    private FotoDatoServicio fotoDatoServicio;


    //Acá están los métodos
    @GetMapping("/inicioAvances")
    public String inicioAvance(
            // @RequestParam(required = false) String obraName,
            @RequestParam(required = false) Long idObraTexto,
            @RequestParam(required = false) Long idObraSelect,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) Long idAPU,
            @RequestParam(required = false) String fecha,

            Model model, org.springframework.security.core.Authentication authentication){

        //Necesito cargar obras para mostrar nombres
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("obras", obras);


        // Start with all avances
        List<Avance> avances = avanceServicio.listaAvance();

        // Apply filters in a more flexible way
        if (idObraSelect != null) {
            avances = avanceServicio.buscarPorIdObra(idObraSelect);
        }
        if (idObraTexto != null) {
            avances = avanceServicio.buscarPorIdObra(idObraTexto);
        }
        if (idUsuario != null && !idUsuario.isEmpty()) {
            avances = avances.stream()
                    .filter(a -> a.getIdUsuario().equals(idUsuario))
                    .collect(Collectors.toList());
        }
        if (fecha != null && !fecha.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(fecha);
                avances = avances.stream()
                        .filter(a -> a.getFechaAvance() != null && a.getFechaAvance().equals(filterDate))
                        .collect(Collectors.toList());
            } catch (DateTimeParseException e) {
                // Handle invalid date format
                model.addAttribute("error", "Formato de fecha inválido");
            }
        }

        // Add the filtered results and parameters back to the model
        model.addAttribute("avances", avances);
        model.addAttribute("idObraSelect", idObraSelect);
        model.addAttribute("idObraTexto", idObraTexto);
        model.addAttribute("idUsuario", idUsuario);
        model.addAttribute("fecha", fecha);
        model.addAttribute("contratistas", contratistaServicio.listarContratistas());
        //Usuarios para envío masivo de correos
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());

        // Agregar información del tipo de autenticación al modelo
        // Determinar si es usuario OAuth2 o LOCAL
        // Obtener el usuario logueado
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        // Verificar el proveedor de autenticación
        boolean esOAuth2 = usuario != null &&
                usuario.getAuthProvider() != null &&
                !"LOCAL".equals(usuario.getAuthProvider());

        model.addAttribute("esOAuth2", esOAuth2);
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());


        return "avances/inicioAvances";
    }



    //Agregar nuevo
    @GetMapping("/agregarAvance")
    public String formAnexarAvance(Model model, org.springframework.security.core.Authentication authentication){
        List<Obra> obras = obraServicio.listaObra();
        List<Apu> apus = apuServicio.listarElementos();
        List<Contratista> contratistas = contratistaServicio.listarContratistas();

        model.addAttribute("avance", new Avance());
        model.addAttribute("obras",obras);
        model.addAttribute("apus",apus);
        model.addAttribute("contratistas", contratistas);

        return "avances/agregarAvance";
    }

    //Función de guardado
    @PostMapping("/salvar")
    public String salvarAvance(
            //Authentication auth, // Add this parameter to get the logged-in user
            @RequestParam Long idObra,
            @RequestParam String fecha,
            @RequestParam Long idApu,
            @RequestParam Double cantidad,
            @RequestParam(required = false) Long idContratista,
            @RequestParam(value = "photoBase64", required = false) String photoBase64,
            @RequestParam(value = "photoCooN", required = false) Double photoCooN,
            @RequestParam(value = "photoCooE", required = false) Double photoCooE,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes redirectAttributes) {

        try {

            System.out.println("=== INICIO GUARDADO AVANCE ===");
            System.out.println("📝 Datos recibidos:");
            System.out.println("   - ID Obra: " + idObra);
            System.out.println("   - ID APU: " + idApu);
            System.out.println("   - Cantidad: " + cantidad);
            System.out.println("   - PhotoBase64: " + (photoBase64 != null ? "SÍ (" + photoBase64.length() + " chars)" : "NO"));
            System.out.println("   - PhotoFile: " + (photoFile != null && !photoFile.isEmpty() ? "SÍ (" + photoFile.getOriginalFilename() + ")" : "NO"));
            System.out.println("   - Coordenadas: N=" + photoCooN + ", E=" + photoCooE);

            // Obtener usuario logueado
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            // Load the full user object from database
            Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username) ;
            System.out.println("Logged in username: " + username);

            // Verificar etapa de la obra
            Obra obra = obraServicio.localizarObra(idObra);
            if (!Obra.EtapaObra.EJECUCION.equals(obra.getEtapa())) {
                redirectAttributes.addFlashAttribute("error",
                        "Solo se pueden registrar avances en obras en etapa EJECUCIÓN. Etapa actual: " + obra.getEtapa());
                return "redirect:/avances/agregarAvance";
            }

            // Verificar que la obra no esté anulada
            if (obra.isAnular()) {
                redirectAttributes.addFlashAttribute("error", "La obra está anulada");
                return "redirect:/avances/agregarAvance";
            }

            // Verificar que la cantidad no sea negativa
            if (cantidad <= 0) {
                redirectAttributes.addFlashAttribute("error", "La cantidad debe ser mayor a cero");
                return "redirect:/avances/agregarAvance";
            }

            // Verificar que la cantidad no exceda lo presupuestado
            Obra presupuesto = obraServicio.obtenerPresupuestoDeObra(idObra);
            if (presupuesto != null) {
                ApusObra apusPresupuesto = presupuesto.getApusObraList().stream()
                        .filter(ap -> ap.getApu().getIdAPU().equals(idApu))
                        .findFirst()
                        .orElse(null);

                if (apusPresupuesto != null) {
                    // Buscar cantidad actual ejecutada
                    ApusObra apusEjecucion = obra.getApusObraList().stream()
                            .filter(ap -> ap.getApu().getIdAPU().equals(idApu))
                            .findFirst()
                            .orElse(null);

                    double ejecutadoActual = apusEjecucion != null && apusEjecucion.getCantidad() != null ?
                            apusEjecucion.getCantidad() : 0.0;
                    double nuevoTotal = ejecutadoActual + cantidad;

                    if (nuevoTotal > apusPresupuesto.getCantidad()) {
                        redirectAttributes.addFlashAttribute("error",
                                String.format("La cantidad excede lo presupuestado. " +
                                                "Presupuestado: %.2f, Ejecutado actual: %.2f, Nuevo total: %.2f",
                                        apusPresupuesto.getCantidad(), ejecutadoActual, nuevoTotal));
                        return "redirect:/avances/agregarAvance";
                    }
                } else {
                    redirectAttributes.addFlashAttribute("error",
                            "La actividad no está en el presupuesto de esta obra");
                    return "redirect:/avances/agregarAvance";
                }
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "No se encontró el presupuesto asociado a esta obra");
                return "redirect:/avances/agregarAvance";
            }

            //Guardar el avance
            Avance avance = new Avance();
            avance.setIdUsuario(usuarioLogeado);
            avance.setIdObra(obraServicio.localizarObra(idObra));
            avance.setFechaAvance(LocalDate.parse(fecha));
            avance.setIdApu(apuServicio.obtenerPorId(idApu));
            avance.setCantEjec(cantidad);
            avance.setAnular(false);

            // ASIGNAR CONTRATISTA SI SE PROPORCIONÓ
            if (idContratista != null && idContratista > 0) {
                Contratista contratista = contratistaServicio.encontrarPorId(idContratista);
                if (contratista != null) {
                    avance.setIdContratista(contratista);
                    System.out.println("Contratista asignado: " + contratista.getNombreContratista());
                } else {
                    System.out.println("⚠Contratista no encontrado con ID: " + idContratista);
                }
            } else {
                System.out.println("ℹNo se asignó contratista");
            }

            avanceServicio.salvar(avance);


            // ACTUALIZAR LA CANTIDAD EJECUTADA EN LA OBRA
            // Buscar el ApusObra correspondiente en la obra en ejecución
            ApusObra apusObra = obra.getApusObraList().stream()
                    .filter(ap -> ap.getApu().getIdAPU().equals(idApu))
                    .findFirst()
                    .orElse(null);

            if (apusObra != null) {
                // Actualizar cantidad ejecutada sumando el nuevo avance
                double nuevaCantidadEjecutada = (apusObra.getCantidad() != null ? apusObra.getCantidad() : 0.0) + cantidad;
                apusObra.setCantidad(nuevaCantidadEjecutada);

                // Guardar la relación actualizada
                obraServicio.actualizar(obra);

                System.out.println("Cantidad ejecutada actualizada para APU " + idApu +
                        ": " + nuevaCantidadEjecutada);
            } else {
                System.err.println("❌ No se encontró ApusObra para APU " + idApu + " en la obra " + idObra);
            }


            //PROCESAR FOTO SI SE PROPORCIONA
            procesarFotoParaAvance(avance, photoBase64, photoFile, photoCooN, photoCooE);
            System.out.println("Avance guardado con ID: " + avance.getIdAvance());

            return "redirect:/avances/inicioAvances";
        } catch (Exception e) {

            e.printStackTrace();
            return "redirect:/avances/agregarAvance?error=Error: " + e.getMessage();

        }

    }

    private void procesarFotoParaAvance(Avance avance, String photoBase64, MultipartFile photoFile,
                                        Double cooN, Double cooE) {
        try {
            // Solo procesar si hay foto
            // Verificar si hay foto para procesar
            boolean tieneFotoBase64 = photoBase64 != null && !photoBase64.isEmpty();
            boolean tienePhotoFile = photoFile != null && !photoFile.isEmpty();

            if (!tieneFotoBase64 && !tienePhotoFile) {
                System.out.println("No hay foto para procesar para el avance ID: " + avance.getIdAvance());
                return;
            }

            // Crear entidad FotoDato
            FotoDato fotoDato = new FotoDato();
            fotoDato.setIdAvance(avance);
            fotoDato.setCooNFoto(cooN);
            fotoDato.setCooEFoto(cooE);
            fotoDato.setFechaFoto(LocalDate.now());

            if (tienePhotoFile) {
                // Procesar archivo subido
                System.out.println("Procesando archivo subido: " + photoFile.getOriginalFilename());
                fotoDatoServicio.salvar(fotoDato, photoFile);
            } else if (tieneFotoBase64) {
                // Procesar foto Base64 desde cámara
                System.out.println("Procesando foto desde cámara (Base64)");
                procesarFotoBase64(fotoDato, photoBase64);
            }
            System.out.println("Foto guardada para avance ID: " + avance.getIdAvance());

        } catch (Exception e) {
            System.err.println("Error al procesar foto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // MÉTOD0 PARA PROCESAR BASE64 (SIMPLIFICADO)
    private void procesarFotoBase64(FotoDato fotoDato, String base64Data) {
        try {
            if (base64Data == null || base64Data.isEmpty()) {
                System.err.println("Datos Base64 vacíos o nulos");
                return;
            }

            // Separar el prefijo de los datos reales
            String[] parts = base64Data.split(",");
            if (parts.length < 2) {
                System.err.println("Formato Base64 inválido");
                return;
            }

            String base64Image = parts[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

            // Determinar tipo MIME
            String mimeType = "image/jpeg";
            String filename = "foto_capturada.jpg";
            if (parts[0].contains("image/png")) {
                mimeType = "image/png";
                filename = "foto_capturada.png";
            }

            // Crear MultipartFile temporal
            MultipartFile multipartFile = new com.example.util.ByteArrayMultipartFile(
                    imageBytes,
                    "file",
                    filename,
                    mimeType
            );

            // Usar el mét0do salvar normal con MultipartFile
            fotoDatoServicio.salvar(fotoDato, multipartFile);

            System.out.println(" Foto Base64 procesada y guardada en GridFS. Tamaño: " + imageBytes.length + " bytes");

        } catch (Exception e) {
            System.err.println("Error al procesar Base64: " + e.getMessage());
            e.printStackTrace();
        }
    }




    //Función y forma de editado TODO esto no aparece en front tiene problemas
    @GetMapping("/cambiar/{idAvance}")
    public String cambiarAvance(@PathVariable Long idAvance, Model model, org.springframework.security.core.Authentication authentication) {
        Avance avance = avanceServicio.localizarAvance(idAvance);
        List<Obra> obras = obraServicio.listaObra();
        List<Apu> apus = apuServicio.listarElementos();

        // CARGAR FOTOS PARA LA EDICIÓN
        List<FotoDato> fotos = fotoDatoServicio.listaFotoDatoAv(avance);

        model.addAttribute("avance", avance);
        model.addAttribute("obras", obras);
        model.addAttribute("apus", apus);
        model.addAttribute("fotos", fotos);

        return "avances/editarAvance"; // Cambia a la vista correcta
    }


    //borrar
    @GetMapping("/borrar/{idAvance}")
    public String borrarAvance(Avance avance) {
        avanceServicio.borrar(avance);
        return "redirect:/avances/inicioAvances";
    }

    @GetMapping("/anular/{idAvance}")
    public String anularAvance(Avance avance) {
        avance.setAnular(true);
        return "redirect:/avances/inicioAvances";
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idAvance}")
    public String actualizarAvance(
        @PathVariable Long idAvance,
        @ModelAttribute Avance avance,
        @RequestParam Double cantidad,
        @RequestParam Long idUsuario,
        @RequestParam Long idObra,
        @RequestParam String fecha,
        @RequestParam Long idAPU,
        BindingResult result,
        Model model) {


        if (result.hasErrors()) {
            return "avances/editarAvance";
        }

        // Get the username from the authentication object
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario));
        avance.setIdObra(obraServicio.localizarObra(idObra));
        avance.setFechaAvance(LocalDate.parse(fecha));
        avance.setIdApu(apuServicio.obtenerPorId(idAPU));
        avance.setCantEjec(cantidad);
        avance.setAnular(false);

        avanceServicio.actualizar(avance);
        return "redirect:/avances/inicioAvances";
    }

    //Ver detalle (sólo lectura)
    @GetMapping("/detalle/{idAvance}")
    public String detalleAvance(@PathVariable Long idAvance, Model model, org.springframework.security.core.Authentication authentication) throws IOException {
        Avance avance = avanceServicio.localizarAvance(idAvance);
        List<Apu> matriz = apuServicio.listarElementos();
        List<Obra> obras = obraServicio.listaObra();

        List<FotoDato> fotos = fotoDatoServicio.listaFotoDatoAv(avance);

        // DEBUG: Verificar qué se está cargando
        System.out.println("=== DEBUG DETALLE AVANCE ===");
        System.out.println("Avance ID: " + idAvance);
        System.out.println("Avance encontrado: " + (avance != null));
        System.out.println("Fotos encontradas: " + (fotos != null ? fotos.size() : "null"));

        if (fotos != null) {
            for (int i = 0; i < fotos.size(); i++) {
                FotoDato foto = fotos.get(i);
                try {
                    byte[] archivoBytes = fotoDatoServicio.obtenerArchivoFoto(foto.getGridfsFileId());
                    System.out.println("Foto " + i + ": ID=" + foto.getIdFotoDato() +
                            ", GridFS ID=" + foto.getGridfsFileId() +
                            ", Bytes=" + (archivoBytes != null ? archivoBytes.length : "NULL") +
                            ", Nombre=" + foto.getNombreArchivo());
                } catch (Exception e) {
                    System.out.println("Foto " + i + ": ID=" + foto.getIdFotoDato() +
                            ", GridFS ID=" + foto.getGridfsFileId() +
                            ", ERROR=" + e.getMessage());

                }
            }
        }

        //el autor original de el avance
        Usuario autor = avance.getIdUsuario();

        //Contratista si lo hay
        Contratista contratista = avance.getIdContratista();

        model.addAttribute("avance", avance);
        model.addAttribute("fotos", fotos);
        model.addAttribute("autor", autor);
        model.addAttribute("contratista", contratista);
        model.addAttribute("actividad", avanceServicio.localizarAvance(idAvance));
        model.addAttribute("obras",obras);
        model.addAttribute("matriz", matriz);
        model.addAttribute("Editando", false); // ← This forces VIEW mode
        return "avances/verAvances";
    }


    //Materiales (para el manejo de la matriz)
    @Autowired
    private APUServicio APUServicio;

    // Reporte de avances por obra
    @GetMapping("/avances/obra/{idObra}/excel")
    public void exportarAvancesObraExcel(@PathVariable Long idObra, HttpServletResponse response) throws IOException {
        Obra obra = obraServicio.localizarObra(idObra);
        List<Avance> avances = avanceServicio.buscarPorIdObra(idObra);

        String nombreArchivo = "avances_" + obra.getNombreObra().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Avances");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Fecha");
        header.createCell(2).setCellValue("Cantidad");
        header.createCell(3).setCellValue("Actividad");
        header.createCell(4).setCellValue("Contratista");

        // Llenar datos
        int fila = 1;
        for (Avance avance : avances) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(avance.getIdAvance());
            row.createCell(1).setCellValue(avance.getFechaAvance().toString());
            row.createCell(2).setCellValue(avance.getCantEjec());
            row.createCell(3).setCellValue(avance.getIdApu().getNombreAPU());
            row.createCell(4).setCellValue(avance.getIdContratista().getNombreContratista());
        }

        libro.write(response.getOutputStream());
        libro.close();
    }

    @GetMapping("/avances/obra/{idObra}/excelCorreo")
    public byte[] generarReporteAvancesExcelmail(String nombreObra) throws IOException {
        List<Obra> obras = obraServicio.findByObraName(nombreObra);
        if (obras.isEmpty()) {
            throw new RuntimeException("No se encontró ninguna oba con el nombre: " + nombreObra);
        }
        Obra obra = obraServicio.localizarObra(obraServicio.findByObraName(nombreObra).get(0).getIdObra());
        //apus de la obra (presupuestados)
        List<Apu> apus = obraServicio.obtenerApusEntidadesPorObra(obra.getIdObra());
        List<ApusObra> apusObraList = obra.getApusObraList();
        //avances da la obra
        List<Avance> avances = avanceServicio.buscarPorIdObra(obraServicio.findByObraNameIgnoreCase(nombreObra).get(0).getIdObra());


        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Avances");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Gestor");
        header.createCell(2).setCellValue("Fecha");
        header.createCell(3).setCellValue("Actividad");
        header.createCell(4).setCellValue("Cantidad");
        header.createCell(5).setCellValue("% Avance");
        header.createCell(6).setCellValue("Contratista");


        // Llenar datos
        int fila = 1;
        for (Avance avance:avances) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(avance.getIdApu().getIdAPU());
            row.createCell(1).setCellValue(avance.getIdUsuario().getNombreUsuario());
            row.createCell(2).setCellValue(avance.getFechaAvance());
            row.createCell(3).setCellValue(avance.getIdApu().getNombreAPU());
            row.createCell(4).setCellValue(avance.getCantEjec());

            double porcentaje = 0;
            for (ApusObra apusObra : apusObraList) {
                if (avance.getIdApu().getIdAPU() == apusObra.getApu().getIdAPU()){
                    porcentaje=(100*avance.getCantEjec())/apusObra.getCantidad();
                }
            }
            row.createCell(5).setCellValue(porcentaje);
            row.createCell(6).setCellValue(avance.getIdContratista().getIdPersona().getNombre() + " " + avance.getIdContratista().getIdPersona().getApellido());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        libro.write(outputStream);
        libro.close();

        return outputStream.toByteArray();
    }

    // Métod0 para obtener APUs por obra (para el dropdown dinámico)
    @GetMapping("/obtenerAPUsPorObra/{idObra}")
    @ResponseBody
    public List<Map<String, Object>> obtenerAPUsPorObra(@PathVariable Long idObra) {

        try {
            Obra obra = obraServicio.localizarObra(idObra);
            System.out.println("Obra encontrada: " + (obra != null ? obra.getNombreObra() : "null"));

            if (obra != null && obra.getApusObraList() != null) {
                List<Map<String, Object>> apusData = obra.getApusObraList().stream()
                        .map(apusObra -> {
                            Apu apu = apusObra.getApu();
                            // Crear un Map con solo los datos que necesitas
                            Map<String, Object> apuMap = new HashMap<>();
                            apuMap.put("idAPU", apu.getIdAPU());
                            apuMap.put("nombreAPU", apu.getNombreAPU());
                            apuMap.put("unidadAPU", apu.getUnidadesAPU());
                            apuMap.put("descripcionAPU", apu.getDescAPU());
                            apuMap.put("vTotalAPU", apu.getVTotalApu());
                            return apuMap;
                        })
                        .collect(Collectors.toList());
                System.out.println("Número de APUs encontrados: " + apusData.size());
                return apusData;
            }

            System.out.println("No se encontraron APUs para la obra");
            return Collections.emptyList();
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error al obtener APUs para obra " + idObra + ": " + e.getMessage());
            return Collections.emptyList();
        }


    }

    // Generar reporte de avances en excel para correo electrónico
    @GetMapping("/excelCorreo")
    public byte[] generarReporteAvancesExcel(@RequestParam(required = false) Long idObraTexto,
                                             @RequestParam(required = false) Long idObraSelect,
                                             @RequestParam(required = false) String idUsuario,
                                             @RequestParam(required = false) Long idAPU,
                                             @RequestParam(required = false) String fecha) throws IOException {
        Obra obra = new Obra();
        List<Avance> avancesObra = new ArrayList<>();
        if(idObraSelect!=null)
        {
            obra = obraServicio.localizarObra(idObraSelect);
            avancesObra = avanceServicio.buscarPorIdObra(idObraSelect);
        }
        if(idObraTexto!=null)
        {
            obra = obraServicio.localizarObra(idObraTexto);
            avancesObra = avanceServicio.buscarPorIdObra(idObraTexto);
        }

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Avances");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("Avances obra - " + obra.getNombreObra());

        header = hoja.createRow(1);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Contratista");
        header.createCell(2).setCellValue("Gestor");
        header.createCell(3).setCellValue("Actividad");
        header.createCell(4).setCellValue("Cantidad");
        header.createCell(5).setCellValue("Fecha");

        // Llenar datos
        int fila = 2;
        for (Avance avance:avancesObra) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(avance.getIdAvance());
            row.createCell(1).setCellValue(avance.getIdContratista().getNombreContratista());
            row.createCell(2).setCellValue(avance.getIdUsuario().getNombreUsuario());
            row.createCell(3).setCellValue(avance.getIdApu().getNombreAPU());
            row.createCell(4).setCellValue(avance.getCantEjec());
            row.createCell(5).setCellValue(avance.getFechaAvance());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        libro.write(outputStream);
        libro.close();

        return outputStream.toByteArray();
    }

    // Reporte para correo masivo
    public byte[] generarReporteAvancesConFiltros(Long idObraSelect, Long idObraTexto, String idUsuario, Long idAPU, String fecha) throws IOException {
        try {
            // Obtener avances con los mismos filtros que en inicioAvances
            List<Avance> avances = avanceServicio.listaAvance();

            // Aplicar filtros de la misma manera que en inicioAvances
            if (idObraSelect != null) {
                avances = avanceServicio.buscarPorIdObra(idObraSelect);
            }
            if (idObraTexto != null) {
                avances = avanceServicio.buscarPorIdObra(idObraTexto);
            }
            if (idUsuario != null && !idUsuario.isEmpty()) {
                avances = avances.stream()
                        .filter(a -> a.getIdUsuario() != null && a.getIdUsuario().getIdUsuario().toString().equals(idUsuario))
                        .collect(Collectors.toList());
            }
            if (fecha != null && !fecha.isEmpty()) {
                try {
                    LocalDate filterDate = LocalDate.parse(fecha);
                    avances = avances.stream()
                            .filter(a -> a.getFechaAvance() != null && a.getFechaAvance().equals(filterDate))
                            .collect(Collectors.toList());
                } catch (DateTimeParseException e) {
                    throw new RuntimeException("Formato de fecha inválido: " + fecha);
                }
            }

            // Crear el reporte Excel
            Workbook libro = new XSSFWorkbook();
            Sheet hoja = libro.createSheet("Avances Filtrados");

            // Crear encabezados
            Row header = hoja.createRow(0);
            header.createCell(0).setCellValue("ID Avance");
            header.createCell(1).setCellValue("ID Obra");
            header.createCell(2).setCellValue("Nombre Obra");
            header.createCell(3).setCellValue("Fecha");
            header.createCell(4).setCellValue("Actividad");
            header.createCell(5).setCellValue("Cantidad Ejecutada");
            header.createCell(6).setCellValue("Usuario");
            header.createCell(7).setCellValue("Contratista");

            // Llenar datos
            int fila = 1;
            for (Avance avance : avances) {
                if (avance != null && !avance.isAnular()) {
                    Row row = hoja.createRow(fila++);
                    row.createCell(0).setCellValue(avance.getIdAvance());
                    row.createCell(1).setCellValue(avance.getIdObra().getIdObra());
                    row.createCell(2).setCellValue(avance.getIdObra() != null && avance.getIdObra().getNombreObra() != null ? avance.getIdObra().getNombreObra() : "");
                    row.createCell(3).setCellValue(avance.getFechaAvance() != null ? avance.getFechaAvance().toString() : "");
                    row.createCell(4).setCellValue(avance.getIdApu() != null && avance.getIdApu().getNombreAPU() != null ? avance.getIdApu().getNombreAPU() : "");
                    row.createCell(5).setCellValue(avance.getCantEjec() != null ? avance.getCantEjec() : 0);
                    row.createCell(6).setCellValue(avance.getIdUsuario() != null && avance.getIdUsuario().getNombreUsuario() != null ? avance.getIdUsuario().getNombreUsuario() : "");
                    row.createCell(7).setCellValue(avance.getIdContratista() != null && avance.getIdContratista().getIdPersona() != null ?
                            avance.getIdContratista().getIdPersona().getNombre() + " " + avance.getIdContratista().getIdPersona().getApellido() : "");
                }
            }

            // Autoajustar columnas
            for (int i = 0; i < 8; i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            libro.write(outputStream);
            libro.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte de avances con filtros: " + e.getMessage(), e);
        }
    }

    //SISTEMA DE AVANCES PARA LAS OBRAS POR ETAPAS
    // Endpoint para ver el progreso de una obra en ejecución
    @GetMapping("/progreso/{idObra}")
    @ResponseBody
    public Map<String, Object> verProgreso(@PathVariable Long idObra) {
        Map<String, Object> progreso = new HashMap<>();

        Obra obra = obraServicio.localizarObra(idObra);
        if (obra == null || !Obra.EtapaObra.EJECUCION.equals(obra.getEtapa())) {
            progreso.put("error", "La obra no está en etapa EJECUCIÓN");
            return progreso;
        }

        Double porcentajeGlobal = obraServicio.calcularPorcentajeAvance(idObra);
        progreso.put("porcentajeGlobal", porcentajeGlobal);
        progreso.put("obra", obra);

        // Detalle por APU
        List<Map<String, Object>> detalles = new ArrayList<>();
        Obra presupuesto = obraServicio.obtenerPresupuestoDeObra(idObra);

        if (presupuesto != null) {
            for (ApusObra apusEjecucion : obra.getApusObraList()) {
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("apu", apusEjecucion.getApu().getNombreAPU());
                detalle.put("ejecutado", apusEjecucion.getCantidad());
                detalle.put("porcentaje", obraServicio.getPorcentajeAvance(apusEjecucion, presupuesto));
                detalle.put("restante", obraServicio.getCantidadRestante(apusEjecucion, presupuesto));
                detalles.add(detalle);
            }
        }
        progreso.put("detalles", detalles);

        return progreso;
    }





}



