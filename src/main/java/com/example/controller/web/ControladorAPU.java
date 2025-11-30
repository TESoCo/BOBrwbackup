package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.MaterialesApu;
import com.example.domain.Usuario;
import com.example.servicio.APUServicio;
import com.example.servicio.MaterialServicio;
import com.example.servicio.MaterialesAPUServicio;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/apu")
public class ControladorAPU {

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private MaterialServicio materialServicio;

    @Autowired
    private MaterialesAPUServicio materialesAPUServicio;

    //INFORMACION DE USUARIO PARA HEADER Y PERMISOS
    private void agregarInfoUsuario(Model model, Authentication authentication){
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
    }

    // ========== MÉTODOS PRINCIPALES ==========
    @GetMapping("/inicioAPU")
    public String inicioAPU(
            @RequestParam(required = false) String search,
            Model model,
            Authentication authentication) {

        List<Apu> apus;

        // Aplicar filtro de búsqueda si se proporciona
        if (getSearch(search) != null && !search.trim().isEmpty()) {
            apus = apuServicio.buscarPorNombre(search);
        } else {
            apus = apuServicio.listarElementos();
        }

        model.addAttribute("apus", apus);
        model.addAttribute("materiales", materialServicio.listarTodos());
        model.addAttribute("searchTerm", search);

        agregarInfoUsuario(model, authentication);

        return "apus/inicioAPU";
    }

    private static String getSearch(String search) {
        return search;
    }

    @GetMapping("/crearAPU")
    public String mostrarFormularioCrear(Model model, Authentication authentication) {

        // Obtener todos los materiales disponibles
        List<Material> materiales = materialServicio.listarTodos();

        // Debug para verificar que los materiales se cargan
        System.out.println("Materiales cargados para crear APU: " + (materiales != null ? materiales.size() : 0));
        if (materiales != null) {
            for (Material material : materiales) {
                System.out.println("Material: " + material.getNombreMaterial() + " - ID: " + material.getIdMaterial());
            }
        }

        model.addAttribute("apu", new Apu());
        model.addAttribute("materiales", materiales);
        agregarInfoUsuario(model, authentication);

        return "apus/crearAPU";
    }

    @PostMapping("/salvar")
    public String salvarAPU(@ModelAttribute Apu apu,
                            @RequestParam(required = false) List<Long> materialIds,
                            @RequestParam(required = false) List<Double> cantidades,
                            BindingResult result,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        System.out.println("=== INICIANDO GUARDADO DE APU ===");
        System.out.println("APU recibido: " + apu.getNombreAPU());
        System.out.println("Material IDs: " + (materialIds != null ? materialIds.size() : 0));
        System.out.println("Cantidades: " + (cantidades != null ? cantidades.size() : 0));

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Errores de validación");
            redirectAttributes.addFlashAttribute("materiales", materialServicio.listarTodos());
            return "redirect:/apu/crearAPU";
        }

        try{
            // Set default values
            setDefaultValues(apu);

            // Asignar usuario actual si es nuevo
            if (apu.getIdAPU() == null && authentication != null) {
                String username = authentication.getName();
                Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
                apu.setIdUsuario(usuario);
                System.out.println("Usuario asignado: " + username);
            }

            // Guardar el APU primero para obtener ID
            apuServicio.guardar(apu);
            Apu apuGuardado = apu;
            System.out.println("APU guardado con ID: " + apuGuardado.getIdAPU());

            // Procesar materiales si se enviaron
            if (materialIds != null && cantidades != null && !materialIds.isEmpty()) {
                procesarMaterialesAPU(apuGuardado, materialIds, cantidades);
                // Recalcular y actualizar el valor de materiales
                BigDecimal totalMateriales = calcularTotalMateriales(apuGuardado);
                apuGuardado.setVMaterialesAPU(totalMateriales);
                System.out.println("Total materiales calculado: " + totalMateriales);
                // Re-guardar el APU con los materiales actualizados
                apuServicio.guardar(apuGuardado);
            } else {
                System.out.println("No se recibieron materiales para procesar");
                // Si no hay materiales, establecer valor en 0
                apuGuardado.setVMaterialesAPU(BigDecimal.ZERO);
                apuServicio.guardar(apuGuardado);
            }

            String mensaje = apu.getIdAPU() == null ? "APU creado correctamente" : "APU actualizado correctamente";
            redirectAttributes.addFlashAttribute("success", mensaje);
            return "redirect:/apu/inicioAPU";

        } catch (Exception e) {
            System.out.println("ERROR al guardar APU: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al guardar APU: " + e.getMessage());
            model.addAttribute("materiales", materialServicio.listarTodos());
            return "redirect:/apu/crearAPU" + (apu.getIdAPU() != null ? "?id=" + apu.getIdAPU() : "");
        }
    }


   ///////////////////////////////////////////////////////////////////////////////////


    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, Authentication authentication) {
        Apu apu = apuServicio.obtenerPorId(id);

        if (apu == null) {
            return "redirect:/apu/inicioAPU?error=APU+no+encontrado";
        }

        // Get available materials
        List<Material> materialesDisponibles = materialServicio.listarTodos();

        // Get current APU materials
        List<MaterialesApu> materialesActuales = materialesAPUServicio.obtenerPorId(id);

        // Extraer cantidades IDs de materiales actuales para el formulario
        Map<Long, Double> cantidadesMateriales = new HashMap<>();
        List<Long> materialIdsActuales = new ArrayList<>();

        for (MaterialesApu ma : materialesActuales) {
            cantidadesMateriales.put(ma.getMaterial().getIdMaterial(), ma.getCantidad());
            materialIdsActuales.add(ma.getMaterial().getIdMaterial());

        }

        model.addAttribute("apu", apu);
        model.addAttribute("materiales", materialServicio.listarTodos());
        model.addAttribute("materialesActuales", materialesActuales);
        model.addAttribute("materialIdsActuales", materialIdsActuales);
        model.addAttribute("cantidadesMap", cantidadesMateriales);

        agregarInfoUsuario(model, authentication);
        return "apus/editarAPU";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleAPU(@PathVariable Long id, Model model,Authentication authentication) {
        try {
            Apu apu = apuServicio.obtenerPorId(id);
            if (apu == null) {
                return "redirect:/apu/inicioAPU?error=APU+no+encontrado";
            }

            model.addAttribute("apu", apu);
            model.addAttribute("SoloLectura", true);
            model.addAttribute("materiales", materialesAPUServicio.obtenerPorId(id));

            agregarInfoUsuario(model, authentication);

            return "apus/detalleAPU";
        } catch (Exception e) {
            return "redirect:/apu/inicioAPU?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarAPU(@PathVariable Long id) {
        apuServicio.eliminar(apuServicio.obtenerPorId(id));
        return "redirect:/apu/inicioAPU";
    }



    // ========== FORMULARIO UNIFICADO PARA CREAR/EDITAR ==========

    @GetMapping("/form")
    public String mostrarFormularioUnificado(
            @RequestParam(required = false) Long id,
            Model model,
            Authentication authentication) {

        Apu apu;
        boolean editando = false;

        if (id != null) {
            apu = apuServicio.obtenerPorId(id);
            editando = true;
        } else {
            apu = new Apu();
        }

        List<Material> materialesDisponibles = materialServicio.listarTodos();

        model.addAttribute("apu", apu);
        model.addAttribute("materiales", materialesDisponibles);
        model.addAttribute("Editando", editando);

        agregarInfoUsuario(model, authentication);
        return "apus/form";
    }





    // ========== MÉTOD0 PARA PROCESAR MATERIALES ==========
    private void procesarMaterialesAPU(Apu apu, List<Long> materialIds, List<Double> cantidades) {
        // Limpiar materiales existentes si estamos editando
        if (apu.getIdAPU() != null) {
            List<MaterialesApu> materialesExistentes = materialesAPUServicio.obtenerPorId(apu.getIdAPU());
            for (MaterialesApu material : materialesExistentes) {
                materialesAPUServicio.eliminar(material);
            }
        }

        // Crear nueva lista si no existe
        if (apu.getMaterialesApus() == null) {
            apu.setMaterialesApus(new ArrayList<>());
        } else {
            apu.getMaterialesApus().clear();
        }

        // Agregar nuevos materiales
        for (int i = 0; i < materialIds.size(); i++) {
            Long materialId = materialIds.get(i);
            Double cantidad = cantidades.get(i);

            if (materialId != null && cantidad != null && cantidad > 0) {
                Material material = materialServicio.obtenerPorId(materialId);
                if (material != null) {
                    MaterialesApu materialesApu = new MaterialesApu();
                    materialesApu.setApu(apu);
                    materialesApu.setMaterial(material);
                    materialesApu.setCantidad(cantidad);

                    // Save the relationship
                    materialesAPUServicio.guardar(materialesApu);
                    apu.getMaterialesApus().add(materialesApu);
                }
            }
        }

        // Recalcular total de materiales
        apu.setVMaterialesAPU(calcularTotalMateriales(apu));
    }

    // ========== MÉTOD0 PARA CALCULAR TOTAL MATERIALES ==========
    private BigDecimal calcularTotalMateriales(Apu apu) {
        if (apu.getMaterialesApus() == null || apu.getMaterialesApus().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (MaterialesApu materialesApu : apu.getMaterialesApus()) {
            if (materialesApu.getMaterial() != null && materialesApu.getCantidad() != null) {
                BigDecimal precio = materialesApu.getMaterial().getPrecioMaterial();
                BigDecimal cantidad = BigDecimal.valueOf(materialesApu.getCantidad());
                if (precio != null) {
                    total = total.add(precio.multiply(cantidad));
                }
            }
        }
        return total;
    }

    // ========== ENDPOINTS ORIGINALES ==========








    @PostMapping("/actualizar/{id}")
    public String actualizarAPU(@PathVariable Long id, @ModelAttribute Apu apu, BindingResult result) {
        if (result.hasErrors()) {
            return "apus/editarAPU";
        }

        // Obtener el APU existente para preservar el ID
        Apu apuExistente = apuServicio.obtenerPorId(id);

        // Actualizar los campos
        apuExistente.setNombreAPU(apu.getNombreAPU());
        apuExistente.setDescAPU(apu.getDescAPU());
        apuExistente.setUnidadesAPU(apu.getUnidadesAPU());
        apuExistente.setVMaterialesAPU(apu.getVMaterialesAPU() != null ? apu.getVMaterialesAPU() : BigDecimal.ZERO);
        apuExistente.setVManoDeObraAPU(apu.getVManoDeObraAPU() != null ? apu.getVManoDeObraAPU() : BigDecimal.ZERO);
        apuExistente.setVTransporteAPU(apu.getVTransporteAPU() != null ? apu.getVTransporteAPU() : BigDecimal.ZERO);
        apuExistente.setVMiscAPU(apu.getVMiscAPU());

        apuServicio.guardar(apuExistente);
        return "redirect:/apu/inicioAPU";
    }

    // ========== IMPORTACIÓN CSV (ORIGINAL) ==========
    @PostMapping("/importar")
    public String importarAPUsDesdeCSV(
            @RequestParam("archivoCSV") MultipartFile archivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor seleccione un archivo CSV");
            return "redirect:/apu/inicioAPU";
        }

        String contentType = archivo.getContentType();
        String originalFilename = archivo.getOriginalFilename();

        System.out.println("Archivo recibido: " + originalFilename);
        System.out.println("Content-Type: " + contentType);
        System.out.println("Tamaño: " + archivo.getSize() + " bytes");

        if (!"text/csv".equals(contentType) &&
                !originalFilename.toLowerCase().endsWith(".csv")) {
            redirectAttributes.addFlashAttribute("error", "El archivo debe ser un CSV válido");
            return "redirect:/apu/inicioAPU";
        }

        try {
            // Get current user
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/apu/inicioAPU";
            }

            // Import APUs from CSV
            List<Apu> apusImportados = apuServicio.importarAPUsDesdeCSV(archivo, usuario);

            // Save all imported APUs
            if (!apusImportados.isEmpty()) {
                apuServicio.guardarTodos(apusImportados);
                redirectAttributes.addFlashAttribute("success",
                        "Se importaron " + apusImportados.size() + " APUs correctamente");
            } else {
                redirectAttributes.addFlashAttribute("warning",
                        "El archivo CSV no contenía datos válidos para importar");
            }


        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al procesar el archivo CSV: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error inesperado: " + e.getMessage());
        }

        return "redirect:/apu/inicioAPU";
    }
    private void setDefaultValues(Apu apu) {
        // Set default values for optional fields if null
        if (apu.getVMaterialesAPU() == null) {
            apu.setVMaterialesAPU(java.math.BigDecimal.ZERO);
        }
        if (apu.getVManoDeObraAPU() == null) {
            apu.setVManoDeObraAPU(java.math.BigDecimal.ZERO);
        }
        if (apu.getVTransporteAPU() == null) {
            apu.setVTransporteAPU(java.math.BigDecimal.ZERO);
        }
    }






}