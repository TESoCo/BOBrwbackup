package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/contratistas")

public class ControladorContratistas {
    @Autowired
    private InventarioServicio inventarioServicio;

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private ContratistaServicio contratistaServicio;

    @Autowired
    private ProveedorServicio proveedorServicio;

    @Autowired
    private PersonaServicio personaServicio;

    @Autowired
    private InfoComServicio infoComServicio;

    @GetMapping
    public String inicioContrat(Model model){
        List<Contratista> contratistas = contratistaServicio.listarContratistas();
        List<InformacionComercial> informacionesComerciales = infoComServicio.comercialList();
        model.addAttribute("contratistas",contratistas);
        model.addAttribute("informacionesComerciales", informacionesComerciales);
        return "contratistas/contratista";
    }


    // Reporte de contratistas
    @GetMapping("/contratistas/excel")
    public void exportarContratistasExcel(HttpServletResponse response) throws IOException {
        List<Contratista> contratistas = contratistaServicio.listarContratistas();
        String nombreArchivo = "reporte_contratistas.xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Contratistas");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("Contratista");
        header.createCell(1).setCellValue("Contacto");
        header.createCell(2).setCellValue("Telefono");
        header.createCell(3).setCellValue("Correo");
        header.createCell(4).setCellValue("Dirección");

        // Llenar datos
        int fila = 1;
        for (Contratista contratista : contratistas) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(contratista.getNombreContratista());
            row.createCell(1).setCellValue(contratista.getIdPersona().getNombre());
            row.createCell(2).setCellValue(contratista.getIdPersona().getTelefono());
            row.createCell(3).setCellValue(contratista.getIdPersona().getCorreo());
            row.createCell(4).setCellValue(contratista.getInformacionComercial().getDireccion());
        }

        libro.write(response.getOutputStream());
        libro.close();
    }

    @GetMapping("/contratistas/excelCorreo")
    public byte[] generarReporteContratistasExcel() throws IOException {
        List<Contratista> contratistas = contratistaServicio.listarContratistas();

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Contratistas");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Nombre Empresa");
        header.createCell(2).setCellValue("Contacto");
        header.createCell(3).setCellValue("Teléfono");
        header.createCell(4).setCellValue("Email");
        header.createCell(5).setCellValue("Dirección");

        // Llenar datos
        int fila = 1;
        for (Contratista contratista : contratistas) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(contratista.getIdContratista());
            row.createCell(1).setCellValue(contratista.getNombreContratista());
            row.createCell(2).setCellValue(contratista.getIdPersona().getNombre() + " " + contratista.getIdPersona().getApellido());
            row.createCell(3).setCellValue(contratista.getIdPersona().getTelefono());
            row.createCell(4).setCellValue(contratista.getIdPersona().getCorreo());
            row.createCell(5).setCellValue(contratista.getInformacionComercial().getDireccion());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        libro.write(outputStream);
        libro.close();

        return outputStream.toByteArray();
    }

    /**
     * Mostrar formulario de registro de contratista
     */
    @GetMapping("/registrar")
    @PreAuthorize("hasAuthority('CREAR_CONTRATISTA')")
    public String mostrarFormularioRegistro(Model model) {

        return "contratistas/registrar";
    }

    /**
     * Procesar registro de nuevo contratista - CREA PERSONA Y CONTRATISTA
     */
    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('CREAR_CONTRATISTA') or hasAuthority('EDITAR_CONTRATISTA')")
    public String guardarContratista(
            @RequestParam(required = false) Long idContratista,
            @RequestParam(required = false) Long idInfoComerc,
            @RequestParam(required = false) Long existingCommercialInfoId,
            @RequestParam(required = false) String originalNitRut,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String telefono,
            @RequestParam String correo,
            @RequestParam String nombreContratista,
            @RequestParam String nitRut,
            @RequestParam (required = false)String formaPago,
            @RequestParam (required = false)String banco,
            @RequestParam (required = false)String numCuenta,
            @RequestParam (required = false)String direccion,
            @RequestParam (required = false)String producto,
            @RequestParam String correoComercial,
            RedirectAttributes ra) {

        // 1. CREAR/ACTUALIZAR LA PERSONA
        Persona persona;
        if (idContratista != null) {
            // Modo edición: usar la persona existente
            Contratista existing = contratistaServicio.encontrarPorId(idContratista);
            persona = existing.getIdPersona();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setTelefono(telefono);
            persona.setCorreo(correo);
        } else {
            // Modo creación: crear nueva persona
            persona = new Persona();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setTelefono(telefono);
            persona.setCorreo(correo);
        }

        // Guardar la persona en la base de datos
        personaServicio.salvar(persona);
        System.out.println("Persona creada con ID: " + persona.getIdPersona());



        /* === 2.  INFO COMERCIAL  === */
        InformacionComercial ic;
        if (idContratista == null) {
            if (existingCommercialInfoId != null) {
                // Usar información comercial existente
                ic = infoComServicio.localizarPorId(idInfoComerc);
            } else {
                // Crear nueva información comercial
                ic = new InformacionComercial();
                // Actualizar datos de información comercial
                ic.setNitRut(nitRut);
                ic.setFormaPago(formaPago);
                ic.setBanco(banco);
                ic.setNumCuenta(numCuenta);
                ic.setDireccion(direccion);
                ic.setProducto(producto);
                ic.setCorreoElectronico(correoComercial);

            }
        } else {
            // Modo edición y usuario quiere guardar la info comercial existente
            Contratista existingContratista = contratistaServicio.encontrarPorId(idContratista);
            ic = contratistaServicio.encontrarPorId(idContratista).getInformacionComercial();


            // Verificar si esta información comercial es usada por otros contratistas
            List<Contratista> contratistaList = contratistaServicio.listarContratistas();
            for (Contratista contratistaExistente : contratistaList) {
                if (contratistaExistente.getIdContratista() - existingContratista.getIdContratista() != 0 && contratistaExistente.getInformacionComercial() == existingContratista.getInformacionComercial()) {
                    boolean esCompartida = true;
                    break;
                }
            }
        }



        infoComServicio.salvar(ic);
        System.out.println("Información comercial " +
                (idInfoComerc != null && idInfoComerc != 0 ? "actualizada" : "creada") +
                " con ID: " + ic.getIdInfoComerc());

        /* === 3.  CONTRATISTA  === */
        Contratista c = (idContratista != null) ? contratistaServicio.encontrarPorId(idContratista)
                : new Contratista();
        c.setNombreContratista(nombreContratista);
        c.setIdPersona(persona);
        c.setInformacionComercial(ic);
        contratistaServicio.guardar(c);

        ra.addFlashAttribute("success",
                idContratista == null ? "Contratista creado" : "Contratista actualizado");
        return "redirect:/contratistas";
    }

    /**
     * Eliminar contratista
     */
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority('EDITAR_USUARIO')")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Contratista contratista = contratistaServicio.encontrarPorId(id);
            if (contratista != null) {

               //  También eliminar la persona asociada
                Persona persona = contratista.getIdPersona();
                contratistaServicio.borrar(contratista);
                System.out.println("Contratista eliminado: " + id);
                if (persona != null) {
                    personaServicio.borrar(persona);
                    System.out.println("Persona eliminada: " + persona.getIdPersona());
                }
                redirectAttributes.addFlashAttribute("success", "Contratista eliminado exitosamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "Contratista no encontrado");
            }



        } catch (Exception e) {
            System.err.println("Error al eliminar: " + e.getMessage());
            e.printStackTrace();

            // Mensaje de error más específico
            if (e.getMessage().contains("constraint") || e.getMessage().contains("foreign key")) {
                redirectAttributes.addFlashAttribute("error", "No se puede eliminar el contratista porque tiene registros asociados en el sistema");
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al eliminar contratista: " + e.getMessage());
            }
        }
        return "redirect:/contratistas";

    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public Contratista apiGet(@PathVariable Long id){
        return contratistaServicio.encontrarPorId(id);
    }


}
