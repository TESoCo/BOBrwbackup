package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/obras")

public class ControladorObras
{
    //Obras
    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private EquipoServicio equipoServicio;


    //Acá están los métodos para presupuestos
    @GetMapping("/inicioObra")
    public String inicioObra(Model model, Authentication authentication){
        // 0. Todas las obras
        List<Obra> obras = obraServicio.listaObra();





        // 1 .FILTRAR OBRAS CON COORDENADAS PARA EL MAPA
        // Filtrar obras que tengan coordenadas (opcional)
        List<Obra> obrasConCoordenadas = obras.stream()
                .filter(obra -> obra.getCooNObra() != null && obra.getCooEObra() != null)
                .collect(Collectors.toList());





        /// ////////////////////////////////////////////////////////////////////
        // 2. FILTROS PARA CONTROL DE ACCESOS POR EQUIPOS
        Usuario usuarioActual = usuarioServicio.encontrarPorNombreUsuario(authentication.getName());
        // VERIFICAR SI EL USUARIO TIENE EQUIPO
        if (usuarioActual.getEquipo() == null) {
            // Si es ADMIN, permitir acceso con mensaje informativo
            if (usuarioActual.getRol() != null && "ADMIN".equals(usuarioActual.getRol().getNombreRol())) {
                model.addAttribute("esAdminSinEquipo", true);
                model.addAttribute("mensajeInfo", "⚠️ Usted es ADMIN pero no tiene equipo asignado. Puede ver todas las obras pero no crear nuevas hasta que se le asigne un equipo.");
            } else {
                // Usuario normal sin equipo - redirigir a página de error
                return "obras/sinEquipoError";
            }
        }
        /// /////////////////////////////////////////////////////////////////////


        // 3. Obtener obras según permisos
        List<Obra> obrasVisibles = obraServicio.obtenerObrasVisibles(usuarioActual);
        System.out.println("Obras visibles para el usuario: " + obrasVisibles.size());



        // 4. Proyectos y obras para la vista agrupada
        List<Proyecto> proyectos = new ArrayList<>();
        if (usuarioActual.getRol() != null && "ADMIN".equals(usuarioActual.getRol().getNombreRol())) {
            proyectos = proyectoServicio.listarProyectos();
        } else {
            // Usuario normal: proyectos de su equipo
            if (usuarioActual.getEquipo() != null) {
                proyectos = proyectoServicio.buscarPorEquipo(usuarioActual.getEquipo().getIdEquipo());
            } else {
                proyectos = new ArrayList<>();
            }
        }



        // 5. Obras sin proyecto (solo las visibles)
        model.addAttribute("obrasSinProyecto", obrasVisibles.stream()
                .filter(o -> o.getProyecto() == null)
                .collect(Collectors.toList()));


        // 6. Verificar si puede crear obra
        boolean puedeCrear = obraServicio.puedeCrearObra(usuarioActual);
        // Mensaje si no puede crear
        if (!puedeCrear) {
            model.addAttribute("mensajeInfo", "Contacte a un administrador para poder crear obras.");
        }

        model.addAttribute("obras", obras); // 0
        model.addAttribute("obrasMapa", obrasConCoordenadas); // 1
        // 2. Filtro por equipos
        model.addAttribute("obrasVisibles", obrasVisibles);// ← Esta es la lista para la tabla // 3
        model.addAttribute("proyectos", proyectos);// 4
        // model.addAttribute("proyectos", proyectoServicio.listarProyectos()); // 4 (lista todos los proyectos si el filtro se daña)
        // 5. Obras sin proyecto
        model.addAttribute("obrasSinProyecto", obraServicio.findByProyectoIsNull()); // 5 (sacado de la capa de servicio
        model.addAttribute("puedeCrearObra", puedeCrear); // 6
        model.addAttribute("equipos", equipoServicio.listarEquipos()); // 7


        return "obras/inicioObra";
    }





    //Agregar nuevo presupuesto
    @GetMapping("/agregarObra")
    public String formAnexarPresupuesto(Model model, Authentication authentication){

        Usuario usuarioActual = usuarioServicio.encontrarPorNombreUsuario(authentication.getName());

        // Verificar que puede crear obra
        if (!obraServicio.puedeCrearObra(usuarioActual)) {
            return "redirect:/obras/inicioObra?error=sinEquipo";
        }

        // Proyectos disponibles para el usuario
        List<Proyecto> proyectosDisponibles = obraServicio.obtenerProyectosDisponibles(usuarioActual);

        // Si el usuario es ADMIN pero no tiene equipo, mostrar mensaje
        if (usuarioActual.getRol() != null && "ADMIN".equals(usuarioActual.getRol().getNombreRol())
                && usuarioActual.getEquipo() == null) {
            model.addAttribute("mensajeInfo", "⚠️ Como ADMIN sin equipo, solo puede ver proyectos. Para crear obras debe tener un equipo asignado.");
            model.addAttribute("proyectos", new ArrayList<>());
        } else {
            model.addAttribute("proyectos", proyectosDisponibles);
        }

        model.addAttribute("obra", new Obra());
        model.addAttribute("APUs", APUServicio.listarElementos());
        model.addAttribute("proyectos", proyectosDisponibles);

        return "obras/agregarObra";
    }






    //Funciónes de guardado y flujo
    // Crear obra en PRESUPUESTO
    @PostMapping("/salvarPresupuesto")
    public String salvarPresupuesto(
            @RequestParam String nombreObra,
            @RequestParam String etapa,
            @RequestParam LocalDate fechaIni,
            @RequestParam LocalDate fechaFin,
            @RequestParam Double cooNObra,
            @RequestParam Double cooEObra,
            @RequestParam List<Long> apuIds,
            @RequestParam List<Double> cantidades,
            @RequestParam(required = false) Long idProyecto,
            RedirectAttributes redirectAttributes,
            Authentication authentication,
            HttpServletRequest request) {

        // Validar input
        System.out.println("=== SALVAR PRESUPUESTO CALLED ===");
        System.out.println("nombreObra: " + nombreObra);
        System.out.println("etapa: " + etapa);
        System.out.println("fechaIni: " + fechaIni);
        System.out.println("fechaFin: " + fechaFin);
        System.out.println("cooNObra: " + cooNObra);
        System.out.println("cooEObra: " + cooEObra);
        System.out.println("apuIds: " + apuIds);
        System.out.println("cantidades: " + cantidades);
        System.out.println("idProyecto: " + idProyecto);

        // Obtener usuario actual
        String username = authentication.getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        try {

            // Validar que el proyecto es obligatorio
            if (idProyecto == null) {
                redirectAttributes.addFlashAttribute("error", "El proyecto es obligatorio. Seleccione un proyecto.");
                return "redirect:/obras/agregarObra";
            }

            // Validar que la etapa sea PRESUPUESTO
            if (!Obra.EtapaObra.PRESUPUESTO.name().equals(etapa)) {
                redirectAttributes.addFlashAttribute("error",
                        "Para crear una obra en PRESUPUESTO use el formulario de presupuesto");
                return "redirect:/obras/agregarObra";
            }

            // Validar tamaños
            if (apuIds.size() != cantidades.size()) {
                redirectAttributes.addFlashAttribute("error",
                        "La cantidad de actividades y cantidades no coincide");
                return "redirect:/obras/agregarObra";
            }

            // Crear mapa de actividades
            Map<Long, Double> actividadesCantidades = new HashMap<>();
            for (int i = 0; i < apuIds.size(); i++) {
                actividadesCantidades.put(apuIds.get(i), cantidades.get(i));
            }

            System.out.println("=== VERIFICANDO PROYECTO ===");
            System.out.println("idProyecto: " + idProyecto);
            Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);
            System.out.println("proyecto encontrado: " + (proyecto != null ? proyecto.getNombreProyecto() : "NULL"));
            System.out.println("proyecto id: " + (proyecto != null ? proyecto.getIdProyecto() : "null"));

            System.out.println("usuario: " + usuario);
            System.out.println("usuario.getIdUsuario(): " + (usuario != null ? usuario.getIdUsuario() : "null"));
            System.out.println("usuario.getEquipo(): " + (usuario != null && usuario.getEquipo() != null ? usuario.getEquipo().getIdEquipo() : "null"));

            // Crear obra en PRESUPUESTO
            Obra obraPresupuesto = obraServicio.crearObraPresupuesto(
                    nombreObra, fechaIni, fechaFin, cooNObra, cooEObra,
                    actividadesCantidades, idProyecto, usuario);

            // ADITORIA
            // Registrar creación
            obraServicio.registrarAuditoriaCreacion(
                    obraPresupuesto,
                    usuario,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar proyecto asignado
            if (proyecto != null) {
                obraServicio.registrarAuditoria(
                        obraPresupuesto,
                        "proyecto",
                        null,
                        proyecto.getNombreProyecto(),
                        usuario,
                        "Proyecto asignado a la obra",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );
            }

            // Registrar APUs asignados
            for (Map.Entry<Long, Double> entry : actividadesCantidades.entrySet()) {
                Apu apu = apuServicio.obtenerPorId(entry.getKey());
                if (apu != null) {
                    obraServicio.registrarAuditoria(
                            obraPresupuesto,
                            "apu_asignado",
                            null,
                            apu.getNombreAPU() + " (x" + entry.getValue() + ")",
                            usuario,
                            "APU asignado al presupuesto",
                            request.getRemoteAddr(),
                            request.getHeader("User-Agent")
                    );
                }
            }


            redirectAttributes.addFlashAttribute("success",
                    "Presupuesto creado exitosamente. ID: " + obraPresupuesto.getIdObra());

            return "redirect:/obras/detalle/" + obraPresupuesto.getIdObra();

        } catch (DataIntegrityViolationException e) {
            System.err.println("=== ERROR DE INTEGRIDAD ===");
            System.err.println("Causa: " + e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Ya existe una obra con ese nombre: " + nombreObra);
            return "redirect:/obras/agregarObra";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear presupuesto: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/obras/agregarObra";
        }
    }

    // Avanzar de PRESUPUESTO a EJECUCIÓN
    @PostMapping("/avanzarAEjecucion/{idObra}")
    public String avanzarAEjecucion(
            @PathVariable Long idObra,
            @RequestParam(required = false) LocalDate fechaInicioReal,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Obtener usuario actual
        String username = authentication.getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        try {
            System.out.println("=== AVANZANDO A EJECUCIÓN ===");
            System.out.println("ID Obra: " + idObra);
            System.out.println("Fecha inicio real: " + fechaInicioReal);

            // Si no se proporciona fecha, usar la fecha actual
            if (fechaInicioReal == null) {
                fechaInicioReal = LocalDate.now();
            }

            // CARGAR CON APUS
            Obra obra = obraServicio.localizarObraConApus(idObra);

            // Verificar si tiene APUs (log para debugging)
            System.out.println("=== APUs EN LA OBRA ===");
            System.out.println("Cantidad de APUs: " + (obra.getApusObraList() != null ? obra.getApusObraList().size() : 0));
            if (obra.getApusObraList() != null) {
                for (ApusObra ao : obra.getApusObraList()) {
                    System.out.println("APU ID: " + (ao.getApu() != null ? ao.getApu().getIdAPU() : "null") +
                            ", Cantidad: " + ao.getCantidad());
                }
            }

            if (!obraServicio.puedeAvanzarAEjecucion(idObra)) {
                redirectAttributes.addFlashAttribute("error",
                        "No se puede avanzar a EJECUCIÓN. Verifique que el presupuesto esté completo.");
                return "redirect:/obras/detalle/" + idObra;
            }

            // Guardar estado anterior para auditoría
            String etapaAnterior = obra.getEtapa().name();

            Obra obraEjecucion = obraServicio.avanzarAEjecucion(idObra, fechaInicioReal, usuario);
            System.out.println("Obra de ejecución creada con ID: " + obraEjecucion.getIdObra());

            // --- AUDITORÍA ---
            // Registrar cambio de estado
            obraServicio.registrarAuditoriaEstado(
                    obraEjecucion,
                    etapaAnterior,
                    "EJECUCIÓN",
                    usuario,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar fecha de inicio real
            obraServicio.registrarAuditoria(
                    obraEjecucion,
                    "fecha_inicio_real",
                    null,
                    fechaInicioReal != null ? fechaInicioReal.toString() : LocalDate.now().toString(),
                    usuario,
                    "Fecha de inicio real de ejecución",
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar relación con obra origen
            obraServicio.registrarAuditoria(
                    obraEjecucion,
                    "obra_origen",
                    null,
                    "ID: " + idObra,
                    usuario,
                    "Obra creada a partir de presupuesto ID: " + idObra,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar en la obra original que fue clonada
            obraServicio.registrarAuditoria(
                    obra,
                    "estado",
                    "PRESUPUESTO",
                    "CLONADO_A_EJECUCION",
                    usuario,
                    "Obra clonada para ejecución",
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            redirectAttributes.addFlashAttribute("success",
                    "Obra avanzada a EJECUCIÓN. ID de ejecución: " + obraEjecucion.getIdObra());

            return "redirect:/obras/detalle/" + obraEjecucion.getIdObra();

        } catch (Exception e) {
            System.err.println("=== ERROR EN AVANZAR A EJECUCIÓN ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al avanzar a EJECUCIÓN: " + e.getMessage());
            return "redirect:/obras/detalle/" + idObra;
        }
    }

    // Avanzar de EJECUCIÓN a CIERRE
    @PostMapping("/avanzarACierre/{idObra}")
    public String avanzarACierre(
            @PathVariable Long idObra,
            @RequestParam(required = false) LocalDate fechaCierreReal,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Obtener usuario actual
        String username = authentication.getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        try {

            System.out.println("=== AVANZANDO A CIERRE ===");
            System.out.println("ID Obra: " + idObra);
            System.out.println("Fecha cierre real: " + fechaCierreReal);

            // Si no se proporciona fecha, usar la fecha actual
            if (fechaCierreReal == null) {
                fechaCierreReal = LocalDate.now();
            }

            if (!obraServicio.puedeAvanzarACierre(idObra)) {
                redirectAttributes.addFlashAttribute("error",
                        "No se puede avanzar a CIERRE. Verifique que todas las actividades estén al 100%.");
                return "redirect:/obras/detalle/" + idObra;
            }

            // Guardar estado anterior para auditoría
            Obra obra = obraServicio.localizarObraConApus(idObra);
            String etapaAnterior = obra.getEtapa().name();

            Obra obraCierre = obraServicio.avanzarACierre(idObra, fechaCierreReal, usuario);
            System.out.println("Obra de cierre creada con ID: " + obraCierre.getIdObra());

            // --- AUDITORÍA ---
            // Registrar cambio de estado
            obraServicio.registrarAuditoriaEstado(
                    obraCierre,
                    etapaAnterior,
                    "CIERRE",
                    usuario,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar fecha de cierre real
            obraServicio.registrarAuditoria(
                    obraCierre,
                    "fecha_cierre_real",
                    null,
                    fechaCierreReal != null ? fechaCierreReal.toString() : LocalDate.now().toString(),
                    usuario,
                    "Fecha de cierre real",
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar porcentaje de avance
            obraServicio.registrarAuditoria(
                    obraCierre,
                    "porcentaje_avance",
                    "0",
                    "100",
                    usuario,
                    "Obra completada al 100%",
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Registrar relación con obra origen
            obraServicio.registrarAuditoria(
                    obraCierre,
                    "obra_origen",
                    null,
                    "ID: " + idObra,
                    usuario,
                    "Obra creada a partir de ejecución ID: " + idObra,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            redirectAttributes.addFlashAttribute("success",
                    "Obra avanzada a CIERRE. ID de cierre: " + obraCierre.getIdObra());

            return "redirect:/obras/detalle/" + obraCierre.getIdObra();

        } catch (Exception e) {
            System.err.println("=== ERROR EN AVANZAR A CIERRE ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al avanzar a CIERRE: " + e.getMessage());
            return "redirect:/obras/detalle/" + idObra;
        }
    }

    // Ver comparativa de obra
    @GetMapping("/comparativa/{idObra}")
    public String verComparativa(@PathVariable Long idObra, Model model) {
        try {
            Map<String, Object> comparativa = obraServicio.obtenerComparativa(idObra);
            model.addAttribute("comparativa", comparativa);

            Obra obra = obraServicio.localizarObra(idObra);
            model.addAttribute("obra", obra);

            // Obtener todas las obras del mismo proyecto
            if (obra.getIdentificadorUnico() != null && !obra.getIdentificadorUnico().isEmpty()) {
                List<Obra> obrasProyecto = obraServicio.findObrasByIdentificador(
                        obra.getIdentificadorUnico());
                model.addAttribute("obrasProyecto", obrasProyecto);
            }

            return "obras/comparativa";

        } catch (Exception e) {
            model.addAttribute("error", "Error al obtener comparativa: " + e.getMessage());
            return "redirect:/obras/detalle/" + idObra;
        }
    }

    // Mostrar todas las obras de un proyecto
    @GetMapping("/proyecto/{idProyecto}")
    public String verObrasProyecto(@PathVariable Long idProyecto, Model model) {
        List<Obra> obras = obraServicio.findByProyectoIdProyecto(idProyecto);
        Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);

        model.addAttribute("obras", obras);
        model.addAttribute("proyecto", proyecto);

        // Obtener obras por etapa
        List<Obra> presupuesto = proyectoServicio.getObrasProyectoPresupuesto(idProyecto);
        List<Obra> ejecucion = proyectoServicio.getObrasProyectoEjecucion(idProyecto);
        List<Obra> cierre = proyectoServicio.getObrasProyectoCierre(idProyecto);

        model.addAttribute("presupuesto", presupuesto);
        model.addAttribute("ejecucion", ejecucion);
        model.addAttribute("cierre", cierre);

        return "obras/obrasProyecto";
    }

    // Verificar si una obra puede avanzar a ejecución (endpoint AJAX)
    @GetMapping("/puedeAvanzar/{idObra}")
    @ResponseBody
    public Map<String, Object> puedeAvanzarAEjecucion(@PathVariable Long idObra) {
        Map<String, Object> response = new HashMap<>();
        response.put("puedeAvanzar", obraServicio.puedeAvanzarAEjecucion(idObra));
        if (response.get("puedeAvanzar").equals(false)) {
            response.put("mensaje", "El presupuesto no está completo o ya existe una ejecución");
        }
        return response;
    }


    //Función y forma de editado
    @GetMapping("/cambiar/{idObra}")
    public String cambiarObra(@PathVariable Long idObra, Model model, Authentication authentication) {
        Obra obra = obraServicio.localizarObra(idObra);

        // Si no tiene APUs, avisar
        if (obra.getApusObraList() == null || obra.getApusObraList().isEmpty()) {
            System.out.println("ERROR: no tiene APUs");
        }

        // Asignar usuario actual
        String username = authentication.getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

        // Create a list of activity IDs and quantities for editing
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(idObra);
        List<Apu> todosApus = APUServicio.listarElementos();
        List<Double> cantidades = new ArrayList<>();
        List<ApusObra> apusObraCant = obra.getApusObraList();

        System.out.println("=== CARGANDO EDICIÓN ===");
        System.out.println("ID Obra: " + idObra);
        System.out.println("Cantidad de APUs en obra: " + (apusObraCant != null ? apusObraCant.size() : 0));
        if (apusObraCant != null) {
            for (ApusObra ao : apusObraCant) {
                System.out.println("  - APU ID: " + (ao.getApu() != null ? ao.getApu().getIdAPU() : "null") +
                        ", Cantidad: " + ao.getCantidad());
            }
        }

        model.addAttribute("obra", obra);
        model.addAttribute("apusObra", apusObra);
        model.addAttribute("listApus", obraServicio.listaObra());
        model.addAttribute("Editando", true); // ← This forces EDIT mode
        model.addAttribute("todosApus", todosApus);
        model.addAttribute("matriz", APUServicio.listarElementos());
        model.addAttribute("apusObraCant", apusObraCant);
        model.addAttribute("cantidades", cantidades);
        model.addAttribute("usuario", usuario);

        // Verificar el proveedor de autenticación para envío de correos
        boolean esOAuth2 = usuario != null &&
                usuario.getAuthProvider() != null &&
                !"LOCAL".equals(usuario.getAuthProvider());

        model.addAttribute("esOAuth2", esOAuth2);


        return "obras/verObras";
    }

    //borrar
    /*@GetMapping("/borrar/{idObra}")
    public String borrarObra(Obra obraBorrar) {
        obraServicio.borrar(obraBorrar);
        return "redirect:/obras/inicioObra";
    }*/



    //anular
    @GetMapping("/anular/{idObra}")
    public String anularObra(
            @PathVariable Long idObra,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

            if (idObra != null && idObra > 0) {
                Obra obraAnular = obraServicio.localizarObra(idObra);

                // Guardar estado anterior para auditoría
                Boolean activoAnterior = obraAnular.getActivo();
                Boolean anularAnterior = obraAnular.isAnular();

                obraAnular.setAnular(true);
                obraAnular.setActivo(false);
                obraServicio.actualizar(obraAnular);

                // --- AUDITORÍA DE ANULACIÓN ---
                obraServicio.registrarAuditoriaAnulacion(
                        obraAnular,
                        usuario,
                        "Obra anulada por usuario",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );

                obraServicio.registrarAuditoria(
                        obraAnular,
                        "activo",
                        activoAnterior ? "true" : "false",
                        "false",
                        usuario,
                        "Obra desactivada",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );

                obraServicio.registrarAuditoria(
                        obraAnular,
                        "anulado",
                        anularAnterior ? "true" : "false",
                        "true",
                        usuario,
                        "Obra marcada como anulada",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );

                redirectAttributes.addFlashAttribute("success",
                        "Obra anulada correctamente. Registro de auditoría creado.");
            } else {
                System.out.println("ERROR: idObra no válido");
                return "redirect:/obras/inicioObra";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al anular la obra: " + e.getMessage());
        }

        return "redirect:/obras/inicioObra";
    }


    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idObra}")
    public String actualizarPresupuesto(
            @PathVariable Long idObra,
            @RequestParam Map<String, String> allParams,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        System.out.println("Iniciando actualización de obra");
        System.out.println("ID Obra: " + idObra);

        // Extraer actividadIds y cantidades del Map
        List<Long> actividadIds = new ArrayList<>();
        List<Double> cantidades = new ArrayList<>();

        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("actividadIds[") && key.endsWith("]")) {
                actividadIds.add(Long.parseLong(value));
            } else if (key.startsWith("cantidades[") && key.endsWith("]")) {
                cantidades.add(Double.parseDouble(value));
            }
        }

        System.out.println("actividadIds extraídos: " + actividadIds);
        System.out.println("cantidades extraídas: " + cantidades);

        System.out.println("TODOS LOS PARÁMETROS RECIBIDOS");
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        try {
            System.out.println("Validando actividadIds");
            if (actividadIds == null || actividadIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Debe agregar al menos una actividad");
                return "redirect:/obras/cambiar/" + idObra;
            }

            // Obtener el usuario autenticado
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
            System.out.println("Usuario autenticado: " + username + " (ID: " + usuario.getIdUsuario() + ")");

            // Obtener obra con APUs actuales para comparar
            Obra obraExistente = obraServicio.localizarObraConApus(idObra);

            // Guardar estado anterior para auditoría
            Map<Long, Double> estadoAnterior = new HashMap<>();
            if (obraExistente.getApusObraList() != null) {
                for (ApusObra ao : obraExistente.getApusObraList()) {
                    if (ao.getApu() != null) {
                        estadoAnterior.put(ao.getApu().getIdAPU(), ao.getCantidad());
                    }
                }
            }

            System.out.println("Servicio actualizarActividadesDeObra iniciando...");
            obraServicio.actualizarActividadesDeObra(idObra, actividadIds, cantidades, usuario);
            System.out.println("Servicio ejecutado correctamente");

            // Obtener obra actualizada para comparar
            Obra obraActualizada = obraServicio.localizarObraConApus(idObra);

            // --- AUDITORÍA DE CAMBIOS  ---
            // 1. Verificar APUs añadidos
            for (ApusObra ao : obraActualizada.getApusObraList()) {
                if (ao.getApu() != null) {
                    Long apuId = ao.getApu().getIdAPU();
                    Double cantidadNueva = ao.getCantidad();
                    Double cantidadAnterior = estadoAnterior.get(apuId);

                    if (cantidadAnterior == null) {
                        // APU añadido
                        obraServicio.registrarAuditoria(
                                obraActualizada,
                                "apu_agregado",
                                null,
                                ao.getApu().getNombreAPU() + " (x" + cantidadNueva + ")",
                                usuario,
                                "Nuevo APU agregado a la obra",
                                request.getRemoteAddr(),
                                request.getHeader("User-Agent")
                        );
                    } else if (!cantidadAnterior.equals(cantidadNueva)) {
                        // Cantidad modificada
                        obraServicio.registrarAuditoria(
                                obraActualizada,
                                "apu_cantidad",
                                "Cantidad anterior: " + cantidadAnterior,
                                "Cantidad nueva: " + cantidadNueva,
                                usuario,
                                "Cantidad modificada para: " + ao.getApu().getNombreAPU(),
                                request.getRemoteAddr(),
                                request.getHeader("User-Agent")
                        );
                    }
                    estadoAnterior.remove(apuId);
                }
            }

            // 2. Verificar APUs eliminados
            for (Map.Entry<Long, Double> entry : estadoAnterior.entrySet()) {
                Long apuId = entry.getKey();
                Double cantidadAnterior = entry.getValue();
                Apu apu = apuServicio.obtenerPorId(apuId);
                if (apu != null) {
                    obraServicio.registrarAuditoria(
                            obraActualizada,
                            "apu_eliminado",
                            apu.getNombreAPU() + " (x" + cantidadAnterior + ")",
                            null,
                            usuario,
                            "APU eliminado de la obra",
                            request.getRemoteAddr(),
                            request.getHeader("User-Agent")
                    );
                }
            }

            redirectAttributes.addFlashAttribute("success", "Obra actualizada correctamente");
            return "redirect:/obras/detalle/" + idObra;

        } catch (Exception e) {
            System.err.println("=== ❌ ERROR EN ACTUALIZACIÓN ===");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Tipo: " + e.getClass().getName());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/obras/cambiar/" + idObra;
        }
    }

    /**
     * Ver historial de auditoría de una obra (igual que inventarios)
     */
    @GetMapping("/auditoria/{idObra}")
    public String verAuditoriaObra(@PathVariable Long idObra, Model model) {
        Obra obra = obraServicio.localizarObra(idObra);
        if (obra == null) {
            model.addAttribute("error", "Obra no encontrada");
            return "redirect:/obras/inicioObra";
        }

        // Usar el mEtHod de ObraServicio que usa AuditoriaDao
        List<Auditoria> auditorias = obraServicio.obtenerAuditoriaPorObra(idObra);

        model.addAttribute("obra", obra);
        model.addAttribute("auditorias", auditorias);
        model.addAttribute("titulo", "Historial de Auditoría - " + obra.getNombreObra());

        return "obras/auditoriaObra";
    }


    //Ver obraDetalle en detalle (sólo lectura)
    @GetMapping("/detalle/{idObra}")
    public String detalleObra(@PathVariable Long idObra, Model model) {
        Obra obra = obraServicio.localizarObra(idObra);
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(idObra);

        List<BigDecimal> valApusObra = new ArrayList<>();
        if(apusObra!=null&& !apusObra.isEmpty())
        {
            for(Apu apu : apusObra)
            {
                valApusObra.add(apuServicio.getPrecioTotalAPU(apu));
            }
        }


        // Calculate total value
        BigDecimal valorTotalEstimado = BigDecimal.ZERO;
        if (obra.getApusObraList() != null) {
            for (ApusObra apusObraItem : obra.getApusObraList()) {
                if (apusObraItem.getApu() != null && apusObraItem.getCantidad() != null) {
                    BigDecimal precioAPU = apuServicio.getPrecioTotalAPU(apusObraItem.getApu());
                    BigDecimal subtotal = precioAPU.multiply(BigDecimal.valueOf(apusObraItem.getCantidad()));
                    valorTotalEstimado = valorTotalEstimado.add(subtotal);
                }
            }
        }

        // Agregar usuarios para el modal de envío de correo
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());

        model.addAttribute("valorTotalEstimado", valorTotalEstimado);
        model.addAttribute("obra", obra);
        model.addAttribute("valApus",valApusObra);
        model.addAttribute("apusObra",apusObra);
        model.addAttribute("Editando", false); // ← This forces VIEW mode

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);
        // Verificar el proveedor de autenticación para envío de correos
        boolean esOAuth2 = usuarioServicio.esUsuarioOAuth2(usuario);


        model.addAttribute("esOAuth2", esOAuth2);

        return "obras/verObras";
    }



    //Materiales (para el manejo de la matriz)
    @Autowired
    private APUServicio APUServicio;

    //Funcionalidad del filtro
    @GetMapping("/filtroPr")
    public String filtroPre(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,

            Model model) {

        List<Obra> obras = new ArrayList<>(); // Initialize with empty list
        Obra obra = null; // Initialize as null
        String error = null;

        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.isEmpty()) {
            switch (tipoBusqueda) {
                case "idObra":
                    Long id = Long.parseLong(valorBusqueda);
                    obra = obraServicio.localizarObra(id);
                    if (obra != null) {
                        obras.add(obra);
                    }
                    break;
                case "nombreObra":
                    obras = obraServicio.findByObraNameContaining(valorBusqueda);
                    break;

                default:
                    obras = obraServicio.listaObra();
            }
        } else {
            obras = obraServicio.listaObra();
        }

        model.addAttribute("obras", obras);
        model.addAttribute("obra", obra);

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "obras/inicioObra";
    }

// ========== NUEVOS MÉTODOS PARA EXPORTACIÓN DE OBRA ==========

    /**
     * Exporta a Excel las actividades (APUs) de una obra específica
     */
    @GetMapping("/exportarObraExcel/{idObra}")
    public void exportarObraExcel(@PathVariable Long idObra, HttpServletResponse response) throws IOException {
        Obra obra = obraServicio.localizarObraConApus(idObra);
        if (obra == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Obra no encontrada");
            return;
        }

        String nombreArchivo = "obra_" + obra.getNombreObra().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Actividades de Obra");

        // Título
        Row titulo = hoja.createRow(0);
        Cell tituloCell = titulo.createCell(0);
        tituloCell.setCellValue("Actividades de la Obra: " + obra.getNombreObra());

        // Estilo para título
        CellStyle titleStyle = libro.createCellStyle();
        Font titleFont = libro.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        tituloCell.setCellStyle(titleStyle);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Información de la obra (fila 2)
        Row infoRow = hoja.createRow(2);
        infoRow.createCell(0).setCellValue("ID Obra:");
        infoRow.createCell(1).setCellValue(obra.getIdObra());
        infoRow.createCell(2).setCellValue("Etapa:");
        infoRow.createCell(3).setCellValue(obra.getEtapa().toString());
        infoRow.createCell(4).setCellValue("Fecha Inicio:");
        infoRow.createCell(5).setCellValue(obra.getFechaIni().toString());
        infoRow.createCell(6).setCellValue("Coordenadas:");
        infoRow.createCell(7).setCellValue("N=" + obra.getCooNObra() + ", E=" + obra.getCooEObra());

        // Fila en blanco
        hoja.createRow(3);

        // Crear encabezados (fila 4)
        Row header = hoja.createRow(4);
        String[] headers = {"ID APU", "Nombre de Actividad", "Unidad", "Cantidad", "Precio Unitario", "Subtotal"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            // Estilo para encabezados
            CellStyle headerStyle = libro.createCellStyle();
            Font headerFont = libro.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos con APUs de la obra
        int fila = 5;
        BigDecimal totalObra = BigDecimal.ZERO;

        List<ApusObra> apusObraList = obra.getApusObraList();
        if (apusObraList != null && !apusObraList.isEmpty()) {
            for (ApusObra apusObra : apusObraList) {
                Apu apu = apusObra.getApu();
                if (apu != null) {
                    Row row = hoja.createRow(fila++);
                    row.createCell(0).setCellValue(apu.getIdAPU());
                    row.createCell(1).setCellValue(apu.getNombreAPU());
                    row.createCell(2).setCellValue(apu.getUnidadesAPU() != null ? apu.getUnidadesAPU() : "");
                    row.createCell(3).setCellValue(apusObra.getCantidad() != null ? apusObra.getCantidad() : 0.0);

                    // Precio unitario del APU
                    BigDecimal precioUnitario = apu.getVTotalApu() != null ? apu.getVTotalApu() : BigDecimal.ZERO;
                    row.createCell(4).setCellValue(precioUnitario.doubleValue());

                    // Subtotal = cantidad * precio unitario
                    BigDecimal cantidad = BigDecimal.valueOf(apusObra.getCantidad() != null ? apusObra.getCantidad() : 0.0);
                    BigDecimal subtotal = precioUnitario.multiply(cantidad);
                    totalObra = totalObra.add(subtotal);
                    row.createCell(5).setCellValue(subtotal.doubleValue());
                }
            }
        }

        // Fila de total
        Row totalRow = hoja.createRow(fila + 1);
        totalRow.createCell(4).setCellValue("TOTAL OBRA:");

        CellStyle totalStyle = libro.createCellStyle();
        Font totalFont = libro.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        Cell totalCell = totalRow.createCell(5);
        totalCell.setCellValue(totalObra.doubleValue());
        totalCell.setCellStyle(totalStyle);

        // Autoajustar columnas
        for (int i = 0; i < headers.length; i++) {
            hoja.autoSizeColumn(i);
        }

        libro.write(response.getOutputStream());
        libro.close();
    }

    /**
     * Genera reporte de la obra en Excel para enviar por correo
     */
    @GetMapping("/generarReporteObraExcelmail/{idObra}")
    public byte[] generarReporteObraExcelmail(@PathVariable Long idObra) throws IOException {
        Obra obra = obraServicio.localizarObraConApus(idObra);
        if (obra == null) {
            throw new RuntimeException("No se encontró la obra con ID: " + idObra);
        }

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Actividades de Obra");

        // Crear encabezados
        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID APU");
        header.createCell(1).setCellValue("Nombre de Actividad");
        header.createCell(2).setCellValue("Unidad");
        header.createCell(3).setCellValue("Cantidad");
        header.createCell(4).setCellValue("Precio Unitario");
        header.createCell(5).setCellValue("Subtotal");

        // Estilo para encabezados
        CellStyle headerStyle = libro.createCellStyle();
        Font headerFont = libro.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < 6; i++) {
            header.getCell(i).setCellStyle(headerStyle);
        }

        // Llenar datos
        int fila = 1;
        BigDecimal totalObra = BigDecimal.ZERO;
        List<ApusObra> apusObraList = obra.getApusObraList();

        if (apusObraList != null && !apusObraList.isEmpty()) {
            for (ApusObra apusObra : apusObraList) {
                Apu apu = apusObra.getApu();
                if (apu != null) {
                    Row row = hoja.createRow(fila++);
                    row.createCell(0).setCellValue(apu.getIdAPU());
                    row.createCell(1).setCellValue(apu.getNombreAPU());
                    row.createCell(2).setCellValue(apu.getUnidadesAPU() != null ? apu.getUnidadesAPU() : "");
                    row.createCell(3).setCellValue(apusObra.getCantidad() != null ? apusObra.getCantidad() : 0.0);

                    BigDecimal precioUnitario = apu.getVTotalApu() != null ? apu.getVTotalApu() : BigDecimal.ZERO;
                    row.createCell(4).setCellValue(precioUnitario.doubleValue());

                    BigDecimal cantidad = BigDecimal.valueOf(apusObra.getCantidad() != null ? apusObra.getCantidad() : 0.0);
                    BigDecimal subtotal = precioUnitario.multiply(cantidad);
                    totalObra = totalObra.add(subtotal);
                    row.createCell(5).setCellValue(subtotal.doubleValue());
                }
            }
        }

        // Fila de total
        Row totalRow = hoja.createRow(fila + 1);
        totalRow.createCell(4).setCellValue("TOTAL OBRA:");
        totalRow.createCell(5).setCellValue(totalObra.doubleValue());

        // Autoajustar columnas
        for (int i = 0; i < 7; i++) {
            hoja.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        libro.write(outputStream);
        libro.close();

        return outputStream.toByteArray();
    }


}