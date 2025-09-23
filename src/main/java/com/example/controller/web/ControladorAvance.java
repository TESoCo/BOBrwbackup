package com.example.controller.web;

import com.example.dao.UsuarioDao;
import com.example.domain.*;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    private UsuarioServicio usuarioServicio; // Use the interface, not implementation


    //Acá están los métodos
    @GetMapping("/inicioAvances")
    public String inicioAvance(
           // @RequestParam(required = false) String obraName,
           @RequestParam(required = false) Long idObraTexto,
           @RequestParam(required = false) Long idObraSelect,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) Long idAPU,
            @RequestParam(required = false) String fecha,
            Model model){


        //Necesito cargar obras para mostrar nombres
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("presupuestos", obras);

//Este if es para las búsquedas por ID

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

        return "avances/inicioAvances";
    }

    //Agregar nuevo
    @GetMapping("/agregarAvance")
    public String formAnexarAvance(Model model){
        List<Obra> obras = obraServicio.listaObra();
        List<Apu> matriz = APUServicio.listarElementos();

        model.addAttribute("avance", new Avance());
        model.addAttribute("obras",obras);
        model.addAttribute("matriz", matriz);
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
            @RequestParam Double cantidad) {

        // Get the currently authenticated user
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // For now, let's just print it to verify it works
        System.out.println("Logged in username: " + username);

        // Load the full user object from database
        Usuario usuarioLogeado = usuarioServicio.encontrarPorId(idUsuario) ;


        Avance avance = new Avance();
        avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario) );
        avance.setIdObra(obraServicio.localizarObra(idObra));
        avance.setFechaAvance(LocalDate.parse(fecha));
        avance.setIdApu(APUServicio.obtenerPorId(idApu));
        avance.setCantEjec(cantidad);


        avanceServicio.salvar(avance);
        return "redirect:/avances/inicioAvances";
    }


    //Función y forma de editado
    /*@GetMapping("/cambiar/{id_avance}")
    public String cambiarAvance(@PathVariable Integer id_avance, Model model) {
        Avance avance = avanceServicio.localizarAvance(id_avance);

        model.addAttribute("avance", avance);
        model.addAttribute("Actividad", avanceServicio.localizarAvance(id_avance));
        model.addAttribute("Editando", true); // ← This forces EDIT mode
        model.addAttribute("matriz", matrizServicio.listarElementos());

        return "avances/verAvances";
    }*/


    //borrar
    @GetMapping("/borrar/{idAvance}")
    public String borrarAvance(Avance avance) {
        avanceServicio.borrar(avance);
        return "redirect:/avances/inicioAvances";
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idAvance}")
    public String actualizarPresupuesto(
        Authentication auth, // Add this parameter to get the logged-in user
        @PathVariable Long idAvance,
        @ModelAttribute Avance avance,
        @RequestParam Double cantidad,
        @RequestParam Long idUsuario,
        @RequestParam Long idObra,
        @RequestParam String fecha,
        BindingResult result,
        @RequestParam Long idApu,
        Model model) {
        if (result.hasErrors()) {
            return "redirect:/avances/cambiar/" + idAvance;
        }

        // Get the username from the authentication object
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario));
        avance.setIdObra(obraServicio.localizarObra(idObra));
        avance.setFechaAvance(LocalDate.parse(fecha));
        avance.setIdApu(APUServicio.obtenerPorId(idApu));
        avance.setCantEjec(cantidad);


        avanceServicio.actualizar(avance);
        return "redirect:/avances/inicioAvances";
    }

    //Ver detalle (sólo lectura)
    @GetMapping("/detalle/{idAvance}")
    public String detalleAvance(@PathVariable Long idAvance, Model model) {
        Avance avance = avanceServicio.localizarAvance(idAvance);
        List<Apu> matriz = APUServicio.listarElementos();
        List<Obra> obras = obraServicio.listaObra();

        model.addAttribute("avance", avance);
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




}



