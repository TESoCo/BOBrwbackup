package com.example.controller.web;

import com.example.domain.Contratista;
import com.example.domain.Obra;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/contratista")
    public String inicioContrat(Model model){
        List<Contratista> contratistas = contratistaServicio.listarContratistas();
        model.addAttribute("contratistas",contratistas);
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


}
