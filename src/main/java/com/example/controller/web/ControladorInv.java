package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.InventarioServicio;
import com.example.servicio.MaterialServicio;
import com.example.servicio.ObraServicio;
import com.example.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/inventarios")
public class ControladorInv {

    @Autowired
    private InventarioServicio inventarioServicio;

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private MaterialServicio materialServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping
    public String inventario(Model model) {
        List<Inventario> inventarios = inventarioServicio.listaInventarios();
        model.addAttribute("inventarios", inventarios);
        return "inventarios/inventario";
    }

    // TODO esto no funciona, 404 en front
    @GetMapping("/crearInv")
    public String crearInv(Model model) {
        // Get the currently logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Load the full user object
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        Inventario inventario = new Inventario();
        inventario.setIdUsuario(usuarioLogeado); // Set the logged-in user

        // Add obras for the dropdown
        List<Obra> obras = obraServicio.listaObra();
        List<Material> materiales = materialServicio.listarTodos();

        model.addAttribute("inventario", inventario);
        model.addAttribute("obras", obras);
        model.addAttribute("materiales", materiales);
        model.addAttribute("usuario", usuarioLogeado);
        return "inventarios/crearInv";
    }

    @Transactional
    @PostMapping("/guardarInv")
    public String guardarInv(@Valid Inventario inventario,
                             Errors errores,
                             Model model,
                             @RequestParam(value = "idObra", required = false) Long obra, // ¡NUEVO PARÁMETRO!
                             @RequestParam(value = "materialIds", required = false) List<Long> materialIds,
                             @RequestParam(value = "materialCantidades", required = false) List<Double> materialCantidades)
    {


// Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        model.addAttribute("usuario", usuarioLogeado); // ¡IMPORTANTE! Agregar el usuario
        model.addAttribute("obras", obraServicio.listaObra());
        model.addAttribute("materiales", materialServicio.listarTodos());
        model.addAttribute("inventario", inventario);

        if (errores.hasErrors()) {
            // Determinar a qué vista regresar según si es nuevo registro o edición
            return (inventario.getIdInventario() == null) ? "inventarios/crearInv" : "inventarios/cambiarInv";
        }

        // Cargar la obra persistente desde la base de datos
        if (obra != null) {
            Obra obraPersistente = obraServicio.localizarObra(obra);
            inventario.setIdObra(obraPersistente);
        } else {
            // Si no hay obra seleccionada, establecer como null
            inventario.setIdObra(null);
        }

        // También asegurar que el usuario sea persistente
        inventario.setIdUsuario(usuarioLogeado);


        // Solo procesar nuevos materiales si se envían en el formulario
        // Esto ocurre normalmente cuando se crea un nuevo inventario o se editan materiales existentes
        if (materialIds != null && materialCantidades != null &&
                materialIds.size() == materialCantidades.size()) {

            // Si es una edición y se envían nuevos materiales, limpiar los existentes
            if (inventario.getIdInventario() != null) {
                inventario.getMaterialesInventarios().clear();
            }

            // Agregar los nuevos materiales
            for (int i = 0; i < materialIds.size(); i++) {
                if (materialIds.get(i) != null) {
                    Material material = materialServicio.obtenerPorId(materialIds.get(i));
                    if (material != null) {
                        MaterialesInventario materialInventario = new MaterialesInventario();
                        materialInventario.setInventario(inventario);
                        materialInventario.setMaterial(material);
                        materialInventario.setCantidad(materialCantidades.get(i));
                        inventario.getMaterialesInventarios().add(materialInventario);
                    }
                }
            }
        }
        // Si no se envían materiales (edición sin cambiar materiales),
        // se mantienen los materiales existentes automáticamente

            // The inventario object already has the usuario set from the form
            inventarioServicio.guardarInv(inventario);

            return "redirect:/inventarios";
    }





    @GetMapping("/verInv")
    public String verInventario(@RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
                                @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
                                Model model) {


        List<Inventario> inventarios = inventarioServicio.listaInventarios();

        String error = null;

        // Verificar si hay parámetros de búsqueda
        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.trim().isEmpty()) {
            // Realizar búsqueda según el tipo
            switch (tipoBusqueda) {
                case "inventarios.idUsuario.nombreUsuario":

                    inventarios = inventarioServicio.buscarPorNombreGestor(valorBusqueda);

                    break;
                case "inventarios.idObra.nombreObra":
                    inventarios = inventarioServicio.buscarPorNombreObra(valorBusqueda);

                    break;
                case "inventarios.fechaIngreso":
                    inventarios = inventarioServicio.buscarPorFecha(valorBusqueda);

                    break;
                default:
                    inventarios = inventarioServicio.listaInventarios();
            }

            if (inventarios.isEmpty()) {
                error = "No se encontraron registros para el criterio de búsqueda '" + valorBusqueda + "'.";
            }

        } else {
            // Si no hay búsqueda, mostrar todos
            inventarios = inventarioServicio.listaInventarios();
        }


        // Pasar datos a la vista
        model.addAttribute("inventarios", inventarios);
        if (error != null) {
            model.addAttribute("error", error);
        }

        // Mantener los valores en el formulario para que se muestren
        model.addAttribute("tipoBusquedaSeleccionado", tipoBusqueda);
        model.addAttribute("valorBusquedaActual", valorBusqueda);


        return "inventarios/verInv";
    }

    @GetMapping("/cambiarInv")
    public String cambiarInv(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
            @RequestParam(value = "id", required = false) Long id, // Nuevo parámetro
            Model model) {

        List<Inventario> inventarios;
        String error = null;

        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.isEmpty()) {
            switch (tipoBusqueda) {
                case "inventarios.idUsuario.nombreUsuario":

                    inventarios = inventarioServicio.buscarPorNombreGestor(valorBusqueda);

                    break;
                case "inventarios.idObra.nombreObra":
                    inventarios = inventarioServicio.buscarPorNombreObra(valorBusqueda);

                    break;
                case "inventarios.fechaIngreso":
                    inventarios = inventarioServicio.buscarPorFecha(valorBusqueda);

                    break;
                default:
                    inventarios = inventarioServicio.listaInventarios();
            }
            if (inventarios.isEmpty()) {
                error = "No se encontraron registros para el criterio de búsqueda '" + valorBusqueda + "'.";
            }

        } else {
            // Si no hay búsqueda, mostrar todos
            inventarios = inventarioServicio.listaInventarios();
        }

        // Si hay un ID específico, cargar ese inventario para editar
        Inventario inventarioParaEditar;
        if (id != null) {
            inventarioParaEditar = inventarioServicio.localizarInventarioPorId(id);
        } else {
            inventarioParaEditar = new Inventario();
        }

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("inventario", inventarioParaEditar);
        // Mantener los valores en el formulario para que se muestren
        model.addAttribute("tipoBusquedaSeleccionado", tipoBusqueda);
        model.addAttribute("valorBusquedaActual", valorBusqueda);


        if (error != null) {
            model.addAttribute("error", error);
        }

        return "inventarios/cambiarInv";
    }

    @GetMapping("/cambiarInv/{idInventario}")
    public String editarInventario(
            @PathVariable("idInventario") Long id,
            Model model) {

        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        model.addAttribute("inventario", inventario);

        List<Inventario> inventarios = inventarioServicio.listaInventarios();
        model.addAttribute("inventarios", inventarios);

        model.addAttribute("obras", obraServicio.listaObra());

        return "inventarios/cambiarInv";
    }

    @GetMapping("/borrarInv")
    public String borrarInv(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
            Model model) {

        List<Inventario> inventarios;

        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.isEmpty()) {
            switch (tipoBusqueda) {
                case "gestor":
                    inventarios = inventarioServicio.buscarPorNombreGestor(valorBusqueda);
                    break;
                case "obra":
                    inventarios = inventarioServicio.buscarPorNombreObra(valorBusqueda);
                    break;
                case "fecha":
                    inventarios = inventarioServicio.buscarPorFecha(valorBusqueda);
                    break;
                default:
                    inventarios = inventarioServicio.listaInventarios();
            }
        } else {
            inventarios = inventarioServicio.listaInventarios();
        }

        model.addAttribute("inventarios", inventarios);
        return "inventarios/borrarInv";
    }


    @GetMapping("/borrarInv/{id_Inventario}")
    public String borrarInventario(
            @PathVariable("id_Inventario") Long id) {
        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        inventarioServicio.borrarInv(inventario);
        return "redirect:/inventarios/inventario";
    }

    //Exportar excel de inventario
    @GetMapping("/exportarExcelInv")
    public void exportarExcelInv(@PathVariable("idInventario") Long id,HttpServletResponse response) throws IOException {

        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        String nombreArchivo = obraServicio.localizarObra(inventario.getIdObra().getIdObra()).getNombreObra().replaceAll("[^a-zA-Z0-9]", "_") + "_" + id + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        List<Inventario> lista = inventarioServicio.listaInventarios();

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Inventarios");

        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("Nombre Del Gestor");
        header.createCell(1).setCellValue("Nombre De La Obra");
        //header.createCell(2).setCellValue("Tipo De Registro");
        header.createCell(2).setCellValue("Fecha");
        header.createCell(3).setCellValue("Unidad De Medida");
        header.createCell(4).setCellValue("Cantidad");
        header.createCell(5).setCellValue("Material");

        int fila = 1;
        for (Inventario inv : lista){
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(inv.getIdUsuario().toString());
            row.createCell(1).setCellValue(inv.getIdObra().toString());
//            row.createCell(2).setCellValue(inv.getTipoRegistro());
            row.createCell(2).setCellValue(inv.getFechaIngreso().toString());
            row.createCell(3).setCellValue(inv.getUnidadInv());

            for (MaterialesInventario mat : inv.getMaterialesInventarios()){
                row.createCell(4).setCellValue(mat.getMaterial().getNombreMaterial());
                row.createCell(5).setCellValue(mat.getCantidad());
                row.createCell(6).setCellValue(mat.getMaterial().getUnidadMaterial());
                row = hoja.createRow(fila++);
            }



        }
        libro.write(response.getOutputStream());
        libro.close();
    }
}