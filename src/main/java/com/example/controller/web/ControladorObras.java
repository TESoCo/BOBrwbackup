package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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


        //model.addAttribute("fotoDatos", new ArrayList<>()); // todo lista real de fotos?
        // TODO: conexion con fotodato



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
            Authentication authentication) {

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

            // Obtener usuario actual
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.encontrarPorNombreUsuario(username);

            System.out.println("=== VERIFICANDO PROYECTO ===");
            System.out.println("idProyecto: " + idProyecto);
            Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);
            System.out.println("proyecto encontrado: " + (proyecto != null ? proyecto.getNombreProyecto() : "NULL"));
            System.out.println("proyecto id: " + (proyecto != null ? proyecto.getIdProyecto() : "null"));

            System.out.println("usuario: " + usuario);
            System.out.println("usuario.getIdUsuario(): " + (usuario != null ? usuario.getIdUsuario() : "null"));
            System.out.println("usuario.getEquipo(): " + (usuario != null && usuario.getEquipo() != null ? usuario.getEquipo().getIdEquipo() : "null"));

            // Crear obra en PRESUPUESTO usando el nuevo métod0 de servicio
            Obra obraPresupuesto = obraServicio.crearObraPresupuesto(
                    nombreObra, fechaIni, fechaFin, cooNObra, cooEObra,
                    actividadesCantidades, idProyecto, usuario);

            // Asignar
            //obraServicio.actualizar(obraPresupuesto);

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
            RedirectAttributes redirectAttributes) {

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


            Obra obraEjecucion = obraServicio.avanzarAEjecucion(idObra, fechaInicioReal, usuario);
            System.out.println("Obra de ejecución creada con ID: " + obraEjecucion.getIdObra());

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
            RedirectAttributes redirectAttributes) {

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

            Obra obraCierre = obraServicio.avanzarACierre(idObra, fechaCierreReal, usuario);
            System.out.println("Obra de cierre creada con ID: " + obraCierre.getIdObra());

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
        Obra presupuesto = obraServicio.getObraPresupuesto(idProyecto);
        Obra ejecucion = obraServicio.getObraEjecucion(idProyecto);
        Obra cierre = obraServicio.getObraCierre(idProyecto);

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
    public String cambiarObra(@PathVariable Long idObra, Model model) {
        Obra obra = obraServicio.localizarObra(idObra);

        // Create a list of activity IDs and quantities for editing
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(idObra);
        List<Apu> todosApus = APUServicio.listarElementos();
        List<Double> cantidades = new ArrayList<>();
        List<ApusObra> apusObraCant = obra.getApusObraList();

        model.addAttribute("obra", obra);
        model.addAttribute("apusObra", apusObra);
        model.addAttribute("listApus", obraServicio.listaObra());
        model.addAttribute("Editando", true); // ← This forces EDIT mode
        model.addAttribute("todosApus", todosApus);
        model.addAttribute("matriz", APUServicio.listarElementos());
        model.addAttribute("apusObraCant", apusObraCant);
        //Map<Integer, Double> actividades = obraEditar.getActiviValues();
        //model.addAttribute("actividadIds", actividadIds);
        model.addAttribute("cantidades", cantidades);

/*
        // Create a Map of Material to Quantity
        Map<Apu, Double> listApus = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : obraServicio.obtenerApusPorObra(idObra)) {
            Apu apuAgregar = APUServicio.obtenerPorId(entry.getKey());
            listApus.put(apuAgregar, entry.getValue());
            apusObra.add(apuAgregar);
            cantidades.add(entry.getValue());
        }
*/

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
    public String anularObra(Long idObra)
    {
        if (idObra !=null && idObra>0) {
            Obra obraAnular = obraServicio.localizarObra(idObra);
            obraAnular.setAnular(true);
            obraServicio.actualizar(obraAnular);
        }else {
            System.out.println("ERROR: idObra no válido");
            return "redirect:/obras/inicioObra";
        }
        return "redirect:/obras/anular/" + idObra;
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idObra}")
    public String actualizarPresupuesto(
            @PathVariable Long idObra,
            @RequestParam(required = false) List<Long> actividadIds,
            @RequestParam(required = false) List<Double> cantidades,

            RedirectAttributes redirectAttributes) {

        try {
            if (actividadIds == null || actividadIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Debe agregar al menos una actividad");
                return "redirect:/obras/cambiar/" + idObra;
            }

            obraServicio.actualizarActividadesDeObra(idObra, actividadIds, cantidades);

            redirectAttributes.addFlashAttribute("success", "Obra actualizada correctamente");
            return "redirect:/obras/detalle/" + idObra;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/obras/cambiar/" + idObra;
        }
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

        /*
        // Create a Map of Material to Quantity
        Map<Apu, Double> listApus = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : obra.obtenerApusPorObra(id_obra)) {
            Apu verAPUobra = APUServicio.obtenerPorId(entry.getKey());
            listApus.put(verAPUobra, entry.getValue());
        }
        */

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

        model.addAttribute("valorTotalEstimado", valorTotalEstimado);
        model.addAttribute("obra", obra);
        model.addAttribute("valApus",valApusObra);
        model.addAttribute("apusObra",apusObra);
        model.addAttribute("Editando", false); // ← This forces VIEW mode
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






}