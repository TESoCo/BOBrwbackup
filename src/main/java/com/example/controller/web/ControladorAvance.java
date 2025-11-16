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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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



        //INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            // Debug información del usuario
            System.out.println("Usuario autenticado: " + username);
            System.out.println("Autoridades: " + authorities);

            // Agregar información específica del usuario al modelo
            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            // Verificar roles específicos
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);
        }

        //Necesito cargar obras para mostrar nombres
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("presupuestos", obras);


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
        //Proveedores para envío masivo de correos
        model.addAttribute("proveedores", proveedorServicio.listar());

        return "avances/inicioAvances";
    }



    //Agregar nuevo
    @GetMapping("/agregarAvance")
    public String formAnexarAvance(Model model, org.springframework.security.core.Authentication authentication){
        List<Obra> obras = obraServicio.listaObra();
        List<Apu> apus = apuServicio.listarElementos();


        model.addAttribute("avance", new Avance());
        model.addAttribute("obras",obras);
        model.addAttribute("apus",apus);

        //INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            // Debug información del usuario
            System.out.println("Usuario autenticado: " + username);
            System.out.println("Autoridades: " + authorities);

            // Agregar información específica del usuario al modelo
            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            // Verificar roles específicos
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);

        }

        return "avances/agregarAvance";
    }

    //Función de guardado
    @PostMapping("/salvar")
    public String salvarAvance(
            //Authentication auth, // Add this parameter to get the logged-in user
            @RequestParam Long idUsuario,
            @RequestParam Long idObra,
            @RequestParam String fecha,
            @RequestParam Long idApu,
            @RequestParam Double cantidad,
            @RequestParam(value = "photoBase64", required = false) String photoBase64,
            @RequestParam(value = "photoCooN", required = false) Double photoCooN,
            @RequestParam(value = "photoCooE", required = false) Double photoCooE,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {

        try {

            System.out.println("=== INICIO GUARDADO AVANCE ===");
            System.out.println("📝 Datos recibidos:");
            System.out.println("   - ID Obra: " + idObra);
            System.out.println("   - ID APU: " + idApu);
            System.out.println("   - Cantidad: " + cantidad);
            System.out.println("   - PhotoBase64: " + (photoBase64 != null ? "SÍ (" + photoBase64.length() + " chars)" : "NO"));
            System.out.println("   - PhotoFile: " + (photoFile != null && !photoFile.isEmpty() ? "SÍ (" + photoFile.getOriginalFilename() + ")" : "NO"));
            System.out.println("   - Coordenadas: N=" + photoCooN + ", E=" + photoCooE);

            // Get the currently authenticated user
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            // Load the full user object from database
            Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username) ;

            // For now, let's just print it to verify it works
            System.out.println("Logged in username: " + username);



            Avance avance = new Avance();
            avance.setIdUsuario(usuarioLogeado );
            avance.setIdObra(obraServicio.localizarObra(idObra));
            avance.setFechaAvance(LocalDate.parse(fecha));
            avance.setIdApu(apuServicio.obtenerPorId(idApu));
            avance.setCantEjec(cantidad);
            avance.setAnular(false);
            avanceServicio.salvar(avance);



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
    // Reemplaza el mét0do procesarFotoBase64 con este código corregido:
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




    //Función y forma de editado
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


        // INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);
        }

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

        // INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);
        }

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
    public List<Apu> obtenerAPUsPorObra(@PathVariable Long idObra) {

        try {
            Obra obra = obraServicio.localizarObra(idObra);
            System.out.println("Obra encontrada: " + (obra != null ? obra.getNombreObra() : "null"));

            if (obra != null && obra.getApusObraList() != null) {
                List<Apu> apus = obra.getApusObraList().stream()
                        .map(ApusObra::getApu)
                        .collect(Collectors.toList());
                System.out.println("Número de APUs encontrados: " + apus.size());
                return apus;
            }

            System.out.println("No se encontraron APUs para la obra");
            return Collections.emptyList();
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error al obtener APUs para obra " + idObra + ": " + e.getMessage());
            return Collections.emptyList();
        }


    }

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
}



