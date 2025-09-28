package com.example.controller.web;

import com.example.domain.Contratista;
import com.example.domain.Material;
import com.example.domain.Proveedor;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/proveedores")

public class ControladorProveedores {
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

    // Reporte de proveedores
    @GetMapping("/proveedores/excel")
    public void exportarProveedoresExcel(HttpServletResponse response) throws IOException {
        List<Proveedor> proveedores = proveedorServicio.listarProveedores();
        String nombreArchivo = "reporte_proveedores.xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Proveedores");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Nombre");
        header.createCell(2).setCellValue("Categoría");
        header.createCell(3).setCellValue("Contacto");
        header.createCell(4).setCellValue("Productos/Servicios");

        // Llenar datos
        int fila = 1;
        for (Proveedor proveedor : proveedores) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(proveedor.getIdProveedor());
            row.createCell(1).setCellValue(proveedor.getIdPersona().getNombre());
            row.createCell(2).setCellValue(proveedor.getInformacionComercial().getDireccion());
            for (Material material : proveedor.getMaterialList()){
                row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(material.getNombreMaterial());
                row.createCell(1).setCellValue(material.getPrecioMaterial().longValue());
                row.createCell(3).setCellValue(material.getUnidadMaterial());

            }


        }

        libro.write(response.getOutputStream());
        libro.close();
    }

    // Reporte de proveedores PARA CORREOS
    @GetMapping("/proveedores/excelCorreo")
    // Métod0 para generar el reporte y devolverlo como byte array
    public byte[] generarReporteProveedoresExcel() throws IOException {
        List<Proveedor> proveedores = proveedorServicio.listarProveedores();

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Proveedores");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Nombre");
        header.createCell(2).setCellValue("Dirección");
        header.createCell(3).setCellValue("Contacto");
        header.createCell(4).setCellValue("Teléfono");
        header.createCell(5).setCellValue("Email");

        // Llenar datos
        int fila = 1;
        for (Proveedor proveedor : proveedores) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(proveedor.getIdProveedor());
            row.createCell(1).setCellValue(proveedor.getIdPersona().getNombre() + " " + proveedor.getIdPersona().getApellido());
            row.createCell(2).setCellValue(proveedor.getInformacionComercial().getDireccion());
            row.createCell(3).setCellValue(proveedor.getIdPersona().getNombre() + " " + proveedor.getIdPersona().getApellido());
            row.createCell(4).setCellValue(proveedor.getIdPersona().getTelefono());
            row.createCell(5).setCellValue(proveedor.getIdPersona().getCorreo());

            // Materiales
            for (Material material : proveedor.getMaterialList()){
                Row materialRow = hoja.createRow(fila++);
                materialRow.createCell(0).setCellValue("Material: " + material.getNombreMaterial());
                materialRow.createCell(1).setCellValue("Precio: " + material.getPrecioMaterial());
                materialRow.createCell(2).setCellValue("Unidad: " + material.getUnidadMaterial());
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        libro.write(outputStream);
        libro.close();

        return outputStream.toByteArray();
    }

}
