package com.example.controller.web;

import com.example.dao.PersonaDao;
import com.example.domain.InformacionComercial;
import com.example.domain.Material;
import com.example.domain.Persona;
import com.example.domain.Proveedor;
import com.example.servicio.InfoComServicio;
import com.example.servicio.PersonaServicio;
import com.example.servicio.ProveedorServicio;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/proveedores")
public class ControladorProveedores {

    private final ProveedorServicio proveedorServicio;
    private final PersonaDao personaDao;
    private final PersonaServicio personaServicio;
    private final InfoComServicio infoComServicio;

    public ControladorProveedores(ProveedorServicio proveedorServicio,
                                  PersonaDao personaDao,
                                  PersonaServicio personaServicio,
                                  InfoComServicio infoComServicio) {
        this.proveedorServicio = proveedorServicio;
        this.personaDao = personaDao;
        this.personaServicio = personaServicio;
        this.infoComServicio = infoComServicio;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        StringTrimmerEditor stringTrimmer = new StringTrimmerEditor(true);
        binder.registerCustomEditor(String.class, stringTrimmer);
    }

    @ModelAttribute("persona")
    public List<?> cargarPersonas() {
        return StreamSupport.stream(personaDao.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    private void setDashboard(Authentication auth, HttpSession session) {
        String destino = "/login?error=sin-permisos";
        if (auth != null) {
            Collection<?> auths = auth.getAuthorities();
            if (auths != null && !auths.isEmpty()) {
                String rol = auths.iterator().next().toString();
                destino = switch (rol) {
                    case "ROLE_ADMINISTRADOR" -> "/dashboardADMIN";
                    case "ROLE_OPERATIVO"     -> "/dashboardOPERA";
                    case "ROLE_SUPERVISOR"    -> "/dashboardSUPER";
                    default -> destino;
                };
            }
        }
        session.setAttribute("dashboardOrigen", destino);
    }

    @GetMapping({"", "/"})
    public String raiz(Model model, Authentication auth, HttpSession session) {
        return inicioProveedor(model, auth, session);
    }

    @GetMapping("/inicioProveedor")
    public String inicioProveedor(Model model, Authentication auth, HttpSession session) {
        setDashboard(auth, session);
        model.addAttribute("proveedores", proveedorServicio.listar());
        return "proveedores/inicioProveedor";
    }

    @GetMapping("/formulario")
    public String nuevo(Model model) {
        Proveedor p = new Proveedor();
        if (p.getInformacionComercial() == null) {
            p.setInformacionComercial(new InformacionComercial());
        }

        // Inicializar persona vacía para el formulario
        if (p.getIdPersona() == null) {
            p.setIdPersona(new Persona());
        }

        // todo: NO inicializar idPersona aquí
        model.addAttribute("proveedor", p);
        model.addAttribute("Editando", false);
        model.addAttribute("informacionesComerciales",infoComServicio.comercialList());
        model.addAttribute("infComProveedor", p.getInformacionComercial());
        return "proveedores/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("proveedor") Proveedor proveedor,
                         @RequestParam(required = false) Long existingCommercialInfoId,
                         @RequestParam(required = false) String originalNitRut,
                         BindingResult result,
                         Model model,
                         RedirectAttributes flash) {

        System.out.println("\n========================================");
        System.out.println("MÉTODO SALVAR EJECUTADO");
        System.out.println("========================================");

        // Debug: Verificar qué llega
        System.out.println("ID Proveedor recibido: " + proveedor.getIdProveedor());
        System.out.println("ID Persona recibido: " + (proveedor.getIdPersona() != null ? proveedor.getIdPersona().getIdPersona() : "NULL"));
        System.out.println("Info Comercial: " + (proveedor.getInformacionComercial() != null ? "Existe" : "NULL"));

        // 1. CREAR/ACTUALIZAR LA PERSONA
        Persona persona;
        if (proveedor.getIdProveedor() != null) {
            // Modo edición: usar la persona existente
            Proveedor existing = proveedorServicio.buscarPorId(proveedor.getIdProveedor())
                    .orElseThrow(() -> new IllegalArgumentException("Proveedor no existe"));
            persona = existing.getIdPersona();
            // Actualizar datos de la persona
            if (proveedor.getIdPersona() != null) {
                persona.setNombre(proveedor.getIdPersona().getNombre());
                persona.setApellido(proveedor.getIdPersona().getApellido());
                persona.setTelefono(proveedor.getIdPersona().getTelefono());
                persona.setCorreo(proveedor.getIdPersona().getCorreo());
            }
        } else {
            // Modo creación: crear nueva persona
            persona = new Persona();
            if (proveedor.getIdPersona() != null) {
                persona.setNombre(proveedor.getIdPersona().getNombre());
                persona.setApellido(proveedor.getIdPersona().getApellido());
                persona.setTelefono(proveedor.getIdPersona().getTelefono());
                persona.setCorreo(proveedor.getIdPersona().getCorreo());
            }
        }



        // Validar persona creada
        if (persona.getNombre() == null || persona.getNombre().trim().isEmpty()) {
            result.rejectValue("idPersona.nombre", "NotEmpty", "El nombre es obligatorio");
        }
        if (persona.getApellido() == null || persona.getApellido().trim().isEmpty()) {
            result.rejectValue("idPersona.apellido", "NotEmpty", "El apellido es obligatorio");
        }
        if (persona.getTelefono() == null || persona.getTelefono().trim().isEmpty()) {
            result.rejectValue("idPersona.telefono", "NotEmpty", "El teléfono es obligatorio");
        }
        if (persona.getCorreo() == null || persona.getCorreo().trim().isEmpty()) {
            result.rejectValue("idPersona.correo", "NotEmpty", "El correo es obligatorio");
        }


        // Validar información comercial
        if (proveedor.getInformacionComercial() == null) {
            result.reject("informacionComercial", "Información comercial es requerida");
            System.out.println("ERROR: Información comercial es null");
        } else {
            InformacionComercial info = proveedor.getInformacionComercial();

            if (info.getCorreoElectronico() == null || info.getCorreoElectronico().trim().isEmpty()) {
                result.rejectValue("informacionComercial.correoElectronico",
                        "NotEmpty", "El correo electrónico es obligatorio");
                System.out.println("ERROR: Email vacío");
            }

            if (info.getProducto() == null || info.getProducto().trim().isEmpty()) {
                result.rejectValue("informacionComercial.producto",
                        "NotEmpty", "El producto/servicio es obligatorio");
                System.out.println("ERROR: Producto vacío");
            }

            if (info.getNitRut() == null || info.getNitRut().trim().isEmpty()) {
                result.rejectValue("informacionComercial.nitRut",
                        "NotEmpty", "El NIT/RUT es obligatorio");
                System.out.println("ERROR: NIT vacío");
            }
        }


        if (proveedor.getInformacionComercial() != null) {
            System.out.println("  - NIT: " + proveedor.getInformacionComercial().getNitRut());
            System.out.println("  - Email: " + proveedor.getInformacionComercial().getCorreoElectronico());
            System.out.println("  - Producto: " + proveedor.getInformacionComercial().getProducto());
        }

        // Si hay errores, volver al formulario
        if (result.hasErrors()) {
            System.out.println("\n=== ERRORES DE VALIDACIÓN ===");
            result.getAllErrors().forEach(error -> {
                System.out.println("  - " + error.getDefaultMessage());
            });
            System.out.println("========================================\n");

            model.addAttribute("Editando", proveedor.getIdProveedor() != null);
            model.addAttribute("proveedor", proveedor);
            return "proveedores/formulario";
        }

        // Intentar guardar
        try {
            System.out.println("\n=== INICIANDO GUARDADO ===");

            // Guardar la persona primero
            personaServicio.salvar(persona);
            System.out.println("Persona creada/actualizada con ID: " + persona.getIdPersona());

            // Asignar la persona al proveedor
            proveedor.setIdPersona(persona);

            // Manejar información comercial existente si se seleccionó
            if (existingCommercialInfoId != null && proveedor.getIdProveedor() == null) {
                InformacionComercial existingInfo = infoComServicio.localizarPorId(existingCommercialInfoId);
                proveedor.setInformacionComercial(existingInfo);
            }


            proveedorServicio.guardar(proveedor);

            System.out.println("=== GUARDADO EXITOSO ===");
            System.out.println("Redirigiendo a /proveedores/inicioProveedor");
            System.out.println("========================================\n");

            flash.addFlashAttribute("ok", proveedor.getIdProveedor() == null
                    ? "Proveedor creado correctamente"
                    : "Proveedor actualizado correctamente");

            return "redirect:/proveedores/inicioProveedor";

        } catch (IllegalArgumentException ex) {
            System.err.println("\n=== ERROR: IllegalArgumentException ===");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            System.err.println("========================================\n");

            result.rejectValue("informacionComercial.nitRut", "duplicado", ex.getMessage());
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("Editando", proveedor.getIdProveedor() != null);
            model.addAttribute("proveedor", proveedor);
            return "proveedores/formulario";

        } catch (Exception ex) {
            System.err.println("\n=== ERROR GENERAL ===");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            System.err.println("========================================\n");

            model.addAttribute("error", "Error al guardar: " + ex.getMessage());
            model.addAttribute("Editando", proveedor.getIdProveedor() != null);
            model.addAttribute("proveedor", proveedor);
            return "proveedores/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Proveedor p = proveedorServicio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no existe"));
        if (p.getInformacionComercial() == null) {
            p.setInformacionComercial(new InformacionComercial());
        }
        model.addAttribute("proveedor", p);
        model.addAttribute("Editando", true);
        model.addAttribute("informacionesComerciales",infoComServicio.comercialList());
        model.addAttribute("infComProveedor", p.getInformacionComercial());
        return "proveedores/formulario";
    }

    @PostMapping("/borrar/{id}")
    public String borrar(@PathVariable Long id, RedirectAttributes flash) {
        proveedorServicio.eliminar(id);
        flash.addFlashAttribute("ok", "Proveedor eliminado");
        return "redirect:/proveedores/inicioProveedor";
    }

    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        return "ok";
    }

    @GetMapping("/proveedores/excel")
    public void exportarProveedoresExcel(HttpServletResponse response) throws IOException {
        List<Proveedor> proveedores = proveedorServicio.listar();
        String nombreArchivo = "reporte_proveedores.xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Proveedores");

        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Nombre");
        header.createCell(2).setCellValue("NIT");
        header.createCell(3).setCellValue("Banco");
        header.createCell(4).setCellValue("Cuenta");
        header.createCell(5).setCellValue("Contacto");
        header.createCell(6).setCellValue("Productos/Servicios");

        int fila = 1;
        for (Proveedor proveedor : proveedores) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(proveedor.getIdProveedor());
            row.createCell(1).setCellValue(proveedor.getIdPersona().getNombre());
            row.createCell(2).setCellValue(proveedor.getInformacionComercial().getNitRut());
            row.createCell(3).setCellValue(proveedor.getInformacionComercial().getBanco());
            row.createCell(4).setCellValue(proveedor.getInformacionComercial().getNumCuenta());
            row.createCell(5).setCellValue(proveedor.getInformacionComercial().getCorreoElectronico());
            row.createCell(6).setCellValue(proveedor.getInformacionComercial().getProducto());
            for (Material material : proveedor.getMaterialList()) {
                row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(material.getNombreMaterial());
                row.createCell(1).setCellValue(material.getPrecioMaterial().longValue());
                row.createCell(3).setCellValue(material.getUnidadMaterial());
            }
        }

        libro.write(response.getOutputStream());
        libro.close();
    }

    @GetMapping("/proveedores/excelCorreo")
    public byte[] generarReporteProveedoresExcel() throws IOException {
        List<Proveedor> proveedores = proveedorServicio.listar();

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Proveedores");

        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Nombre");
        header.createCell(2).setCellValue("Dirección");
        header.createCell(3).setCellValue("Contacto");
        header.createCell(4).setCellValue("Teléfono");
        header.createCell(5).setCellValue("Email");

        int fila = 1;
        for (Proveedor proveedor : proveedores) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(proveedor.getIdProveedor());
            row.createCell(1).setCellValue(proveedor.getIdPersona().getNombre() + " " + proveedor.getIdPersona().getApellido());
            row.createCell(4).setCellValue(proveedor.getIdPersona().getTelefono());
            row.createCell(5).setCellValue(proveedor.getIdPersona().getCorreo());

            for (Material material : proveedor.getMaterialList()) {
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