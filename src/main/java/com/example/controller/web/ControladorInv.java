package com.example.controller.web;

import com.example.dao.PrecioMaterialDao;
import com.example.domain.*;
import com.example.domain.enums.EstadoInventario;
import com.example.servicio.InventarioServicio;
import com.example.servicio.MaterialServicio;
import com.example.servicio.ObraServicio;
import com.example.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private PrecioMaterialDao precioMaterialDao;

    @Autowired
    private HttpServletRequest request;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    // ========== LISTADO PRINCIPAL CON FILTROS ==========
    @GetMapping
    public String inventario(@RequestParam(value = "estado", required = false) String estadoFiltro,
                             @RequestParam(value = "obraId", required = false) Long obraId,
                             Model model) {
        // Obtener usuario actual
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        // Obtener inventarios según filtros
        List<Inventario> inventarios = obtenerInventariosConFiltros(estadoFiltro, obraId, usuarioLogeado);

        // Materiales con precios actuales
        List<Material> materiales = materialServicio.listarTodosConPrecios();

        // Obras para filtro
        List<Obra> obras = obraServicio.listaObra();

        // Estados disponibles para filtro
        List<EstadoInventario> estados = Arrays.asList(EstadoInventario.values());

        // Crear un mapa con los precios actuales pre-calculados
        Map<Long, BigDecimal> preciosActuales = new HashMap<>();
        for (Material material : materiales) {
            BigDecimal precio = materialServicio.getPrecioActual(material.getIdMaterial());
            preciosActuales.put(material.getIdMaterial(), precio);
        }

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("materiales", materiales);
        model.addAttribute("obras", obras);
        model.addAttribute("estados", estados);
        model.addAttribute("estadoFiltroSeleccionado", estadoFiltro);
        model.addAttribute("obraIdSeleccionada", obraId);
        model.addAttribute("usuarioActual", usuarioLogeado);
        model.addAttribute("preciosActuales", preciosActuales);
        return "inventarios/inventario";
    }


    private List<Inventario> obtenerInventariosConFiltros(String estadoFiltro, Long obraId, Usuario usuario) {
        List<Inventario> todos = inventarioServicio.listaInventarios();

        // Filtrar por estado
        if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
            try {
                EstadoInventario estado = EstadoInventario.fromCodigo(estadoFiltro);
                todos = todos.stream()
                        .filter(i -> i.getAprobacion() == estado)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Estado inválido, ignorar filtro
            }
        }
        // Filtrar por obra
        if (obraId != null) {
            todos = todos.stream()
                    .filter(i -> i.getIdObra() != null &&
                            i.getIdObra().getIdObra().equals(obraId))
                    .collect(Collectors.toList());
        }

        // Si no es ADMIN, mostrar solo sus inventarios y los de su equipo

        if (!usuario.getRol().getNombreRol().equals("ADMIN")) {
            Set<Long> obrasPermitidas = obtenerObrasPermitidas(usuario);
            todos = todos.stream()
                    .filter(i -> i.getIdUsuario() != null &&
                            (i.getIdUsuario().getIdUsuario().equals(usuario.getIdUsuario()) ||
                                    (i.getIdObra() != null && obrasPermitidas.contains(i.getIdObra().getIdObra()))))
                    .collect(Collectors.toList());
        }

        return todos;
    }


    // ========== CREAR INVENTARIO ==========

    @GetMapping("/crearInv")
    public String crearInv(Model model) {
        // Get the currently logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Load the full user object
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        Inventario inventario = new Inventario();
        inventario.setIdUsuario(usuarioLogeado); // Set the logged-in user

        inventario.setAprobacion(EstadoInventario.SOLICITADO); // Estado inicial
        inventario.setFechaIngreso(LocalDate.now());

        // Add obras for the dropdown
        List<Obra> obras = obraServicio.listaObra();
        List<Material> materiales = materialServicio.listarTodosConPrecios();

        model.addAttribute("inventario", inventario);
        model.addAttribute("obras", obras);
        model.addAttribute("materiales", materiales);
        model.addAttribute("usuario", usuarioLogeado);
        model.addAttribute("estados", EstadoInventario.values());
        model.addAttribute("esCreacion", true);

        return "inventarios/crearInv";
    }

    // ========== GUARDAR INVENTARIO ==========
    @Transactional
    @PostMapping("/guardarInv")
    public String guardarInv(@Valid Inventario inventario,
                             Errors errores,
                             Model model,
                             @RequestParam(value = "idObra", required = false) Long obra,
                             @RequestParam(value = "materialIds", required = false) List<Long> materialIds,
                             @RequestParam(value = "materialCantidades", required = false) List<Double> materialCantidades,
                             @RequestParam(value = "tipoDestino", required = false) String tipoDestino,
                             RedirectAttributes redirectAttributes)
    {
        // LOG para depuración
        System.out.println("=== DEBUG GUARDAR INVENTARIO ===");
        System.out.println("Inventario: " + inventario);
        System.out.println("tipoDestino: " + tipoDestino);
        System.out.println("idObra recibido: " + obra);
        System.out.println("materialIds: " + materialIds);
        System.out.println("materialCantidades: " + materialCantidades);
        System.out.println("Errores: " + (errores.hasErrors() ? errores.getAllErrors() : "NINGUNO"));

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        // Validar permisos para crear
        if (!puedeCrearInventario(usuarioLogeado)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para crear inventarios");
            return "redirect:/inventarios";
        }



        // Validar errores
        if (errores.hasErrors()) {
            model.addAttribute("usuario", usuarioLogeado);
            model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
            model.addAttribute("materiales", materialServicio.listarTodosConPrecios());
            model.addAttribute("estados", EstadoInventario.values());
            model.addAttribute("esCreacion", inventario.getIdInventario() == null);
            return (inventario.getIdInventario() == null) ? "inventarios/crearInv" : "inventarios/cambiarInv";
        }


        // ========== LÓGICA DE DESTINO ==========
        boolean esParaStock = "STOCK".equals(tipoDestino);

        if (esParaStock) {
            // Caso 1: Agregar al stock (sin obra)
            inventario.setIdObra(null);
            System.out.println("=== AGREGANDO AL STOCK ===");
        } else {
            // Caso 2: Consumir de stock para una obra
            if (obra == null) {
                model.addAttribute("error", "Debe seleccionar una obra");
                model.addAttribute("usuario", usuarioLogeado);
                model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
                model.addAttribute("materiales", materialServicio.listarTodosConPrecios());
                return "inventarios/crearInv";
            }

            Obra obraAsignada = obraServicio.localizarObra(obra);
            if (obraAsignada != null && tieneAccesoAObra(usuarioLogeado, obraAsignada)) {
                inventario.setIdObra(obraAsignada);
            } else {
                model.addAttribute("error", "No tiene acceso a la obra seleccionada");
                return "inventarios/crearInv";
            }
            System.out.println("=== CONSUMIENDO DE STOCK PARA OBRA: " + obraAsignada.getNombreObra());
        }

        // Configurar el inventario
        inventario.setIdUsuario(usuarioLogeado);
        inventario.setAprobacion(EstadoInventario.SOLICITADO);

        // ========== PROCESAR MATERIALES Y STOCK ==========
        if (inventario.getMaterialesInventarios() == null) {
            inventario.setMaterialesInventarios(new ArrayList<>());
        }

        for (int i = 0; i < materialIds.size(); i++) {
            Long materialId = materialIds.get(i);
            Double cantidad = (i < materialCantidades.size()) ? materialCantidades.get(i) : null;

            if (materialId != null && cantidad != null && cantidad > 0) {
                Material material = materialServicio.obtenerPorId(materialId);
                if (material == null) continue;

                // ========== ACTUALIZAR STOCK ==========
                Double stockActual = material.getStockDisponible() != null ?
                        material.getStockDisponible() : 0.0;

                if (esParaStock) {
                    // AGREGAR al stock
                    material.setStockDisponible(stockActual + cantidad);
                    System.out.println("Material: " + material.getNombreMaterial() +
                            " - Stock anterior: " + stockActual +
                            " + " + cantidad +
                            " = " + material.getStockDisponible());
                } else {
                    // CONSUMIR del stock
                    if (stockActual < cantidad) {
                        model.addAttribute("error",
                                String.format("Stock insuficiente para %s. Disponible: %.2f, Solicitado: %.2f",
                                        material.getNombreMaterial(),
                                        stockActual,
                                        cantidad));
                        model.addAttribute("usuario", usuarioLogeado);
                        model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
                        model.addAttribute("materiales", materialServicio.listarTodosConPrecios());
                        return "inventarios/crearInv";
                    }
                    material.setStockDisponible(stockActual - cantidad);
                    System.out.println("Material: " + material.getNombreMaterial() +
                            " - Stock anterior: " + stockActual +
                            " - " + cantidad +
                            " = " + material.getStockDisponible());
                }

                // Guardar el material actualizado
                materialServicio.guardar(material);

                // Crear la relación MaterialesInventario
                MaterialesInventario materialInventario = new MaterialesInventario();
                materialInventario.setInventario(inventario);
                materialInventario.setMaterial(material);
                materialInventario.setCantidad(cantidad);
                inventario.getMaterialesInventarios().add(materialInventario);
            }
        }

        try {
            // Validar que haya al menos un material
            if (inventario.getMaterialesInventarios().isEmpty()) {
                model.addAttribute("error", "Debe seleccionar al menos un material con cantidad válida");
                model.addAttribute("usuario", usuarioLogeado);
                model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
                model.addAttribute("materiales", materialServicio.listarTodosConPrecios());
                return "inventarios/crearInv";
            }

            // Asignar obra si se seleccionó
            if (obra != null) {
                Obra obraAsignada = obraServicio.localizarObra(obra);
                if (obraAsignada != null && tieneAccesoAObra(usuarioLogeado, obraAsignada)) {
                    inventario.setIdObra(obraAsignada);
                } else {
                    model.addAttribute("error", "No tiene acceso a la obra seleccionada");
                    return "inventarios/crearInv";
                }
            }

            // Guardar inventario
            System.out.println("=== GUARDANDO INVENTARIO ===");
            inventarioServicio.guardarInv(inventario);
            System.out.println("=== INVENTARIO GUARDADO CON ID: " + inventario.getIdInventario());

            // Limpiar materiales existentes (para edición)
            if (inventario.getIdInventario() != null) {
                inventario.getMaterialesInventarios().clear();
            }

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



            System.out.println("=== ANTES DE GUARDAR OTRA VEZ ===");

            // The inventario object already has the usuario set from the form
            // Guardar inventario
            inventarioServicio.guardarInv(inventario);

            System.out.println("=== DESPUÉS DE GUARDAR ===");

            // Registrar auditoría de creación
            // Registrar auditoría
            String accion = esParaStock ? "Ingreso de materiales al STOCK" : "Solicitud de materiales para OBRA";
            inventarioServicio.registrarAuditoria(
                    inventario,
                    null,
                    EstadoInventario.SOLICITADO.name(),
                    usuarioLogeado,
                    "Creación de solicitud de inventario: " + (inventario.getComentarioInv() != null ? inventario.getComentarioInv() : "")
            );

            redirectAttributes.addFlashAttribute("success",
                    String.format("%s #%d creada exitosamente en estado %s",
                            esParaStock ? "Ingreso al stock" : "Solicitud de inventario",
                            inventario.getIdInventario(),
                            inventario.getAprobacion().getDisplayName()));



        } catch (Exception e) {
            System.err.println("=== ERROR AL GUARDAR ===");
            e.printStackTrace();
            model.addAttribute("error", "Error al guardar: " + e.getMessage());
            model.addAttribute("usuario", usuarioLogeado);
            model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
            model.addAttribute("materiales", materialServicio.listarTodosConPrecios());
            return "inventarios/crearInv";
        }

        return "redirect:/inventarios";
    }

    // ========== CAMBIAR ESTADO DEL INVENTARIO ==========
    @PostMapping("/{id}/cambiarEstado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam("nuevoEstado") String nuevoEstadoStr,
            @RequestParam(value = "comentario", required = false) String comentario,
            RedirectAttributes redirectAttributes) {

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        try {
            EstadoInventario nuevoEstado = EstadoInventario.fromCodigo(nuevoEstadoStr);

            // Verificar permisos y ejecutar cambio
            Inventario inventario = inventarioServicio.cambiarEstado(id, nuevoEstado, usuarioLogeado, comentario, request.getRemoteAddr(), request.getHeader("User-Agent"));

            redirectAttributes.addFlashAttribute("success",
                    String.format("Estado del inventario #%d cambiado a: %s",
                            id,
                            nuevoEstado.getDisplayName()));

        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado: " + e.getMessage());
        }

        return "redirect:/inventarios";
    }


    // ========== VER INVENTARIO (DETALLE) ==========
    @GetMapping("/verInv")
    public String verInventario(@RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
                                @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
                                @RequestParam(value = "estado", required = false) String estadoFiltro,
                                Model model) {

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

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

        } else if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
            // Filtrar por estado
            try {
                EstadoInventario estado = EstadoInventario.fromCodigo(estadoFiltro);
                 inventarios = inventarioServicio.buscarPorAprobacion(estado);

            } catch (IllegalArgumentException e) {
                inventarios = inventarioServicio.listaInventarios();
            }
        } else {
            inventarios = obtenerInventariosVisibles(usuarioLogeado);
        }

        //Materiales con precios actuales
        List<Material> materiales = materialServicio.listarTodosConPrecios();

        // Preparar datos para el detalle
        Map<Long, List<AuditoriaInventario>> auditoriasMap = new HashMap<>();
        for (Inventario inv : inventarios) {
            auditoriasMap.put(inv.getIdInventario(),
                    inventarioServicio.obtenerAuditoriaPorInventario(inv.getIdInventario()));
        }


        // Pasar datos a la vista
        model.addAttribute("materiales", materiales);
        model.addAttribute("inventarios", inventarios);
        model.addAttribute("auditoriasMap", auditoriasMap);
        model.addAttribute("estados", EstadoInventario.values());
        model.addAttribute("estadoFiltroSeleccionado", estadoFiltro);
        model.addAttribute("tipoBusquedaSeleccionado", tipoBusqueda);
        model.addAttribute("valorBusquedaActual", valorBusqueda);
        if (error != null) {
            model.addAttribute("error", error);
        }

        return "inventarios/verInv";
    }

    // ========== VER DETALLE COMPLETO DE UN INVENTARIO ==========
    @GetMapping("/detalle/{id}")
    public String verDetalleInventario(@PathVariable Long id, Model model) {
        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);

        if (inventario == null) {
            model.addAttribute("error", "Inventario no encontrado");
            return "redirect:/inventarios";
        }

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        // Verificar acceso
        if (!tieneAccesoAlInventario(usuarioLogeado, inventario)) {
            model.addAttribute("error", "No tiene acceso a este inventario");
            return "redirect:/inventarios";
        }

        // Obtener auditoría
        List<AuditoriaInventario> auditoria = inventarioServicio.obtenerAuditoriaPorInventario(id);

        // Obtener estados permitidos para este usuario
        List<EstadoInventario> estadosPermitidos = inventarioServicio.obtenerEstadosPermitidos(id, usuarioLogeado);

        // Materiales con precios
        List<Material> materiales = materialServicio.listarTodosConPrecios();

        model.addAttribute("inventario", inventario);
        model.addAttribute("auditoria", auditoria);
        model.addAttribute("estadosPermitidos", estadosPermitidos);
        model.addAttribute("materiales", materiales);
        model.addAttribute("usuarioActual", usuarioLogeado);
        model.addAttribute("esAdmin", esAdmin(usuarioLogeado)); //cambiar por permisos de aprobación y entrega
        model.addAttribute("estados", EstadoInventario.values());

        return "inventarios/detalleInv";

    }


    @GetMapping("/cambiarInv")
    public String cambiarInv(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
            @RequestParam(value = "id", required = false) Long id, // Nuevo parámetro
            Model model) {

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

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
            // Si no hay búsqueda, mostrar todos los inv a los que pueda acceder el usuario
            inventarios = obtenerInventariosVisibles(usuarioLogeado);
        }

        // Si hay un ID específico, cargar ese inventario para editar
        Inventario inventarioParaEditar;
        if (id != null) {
            inventarioParaEditar = inventarioServicio.localizarInventarioPorId(id);
            // Verificar que el inventario no esté en estado final
            if (inventarioParaEditar != null && inventarioParaEditar.isEstadoFinal()) {
                model.addAttribute("error",
                        "El inventario está en estado " + inventarioParaEditar.getAprobacion().getDisplayName() +
                                " y no puede ser editado");
                return "redirect:/inventarios";
            }
        } else {
            inventarioParaEditar = new Inventario();
        }

        List<Material> materialesConPrecios = materialServicio.listarTodosConPrecios();
        Map<Long, BigDecimal> preciosActualesMap = new HashMap<>();
        for (Material material : materialesConPrecios) {
            preciosActualesMap.put(material.getIdMaterial(), material.getPrecioActual());
        }

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("inventario", inventarioParaEditar);
        model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));
        model.addAttribute("materiales", materialesConPrecios);
        model.addAttribute("preciosActualesMap", preciosActualesMap);
        model.addAttribute("precios", precioMaterialDao.findAll());
        model.addAttribute("estados", EstadoInventario.values());
        model.addAttribute("tipoBusquedaSeleccionado", tipoBusqueda);
        model.addAttribute("valorBusquedaActual", valorBusqueda);
        model.addAttribute("esAdmin", esAdmin(usuarioLogeado));


        if (error != null) {
            model.addAttribute("error", error);
        }

        return "inventarios/cambiarInv";
    }

    @GetMapping("/cambiarInv/{idInventario}")
    public String editarInventario(
            @PathVariable("idInventario") Long id,
            Model model) {

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);

        if (inventario == null) {
            model.addAttribute("error", "Inventario no encontrado");
            return "redirect:/inventarios";
        }

        if (!tieneAccesoAlInventario(usuarioLogeado, inventario)) {
            model.addAttribute("error", "No tiene acceso a este inventario");
            return "redirect:/inventarios";
        }

        // No permitir editar estados finales
        if (inventario.isEstadoFinal()) {
            model.addAttribute("error",
                    "El inventario está en estado " + inventario.getAprobacion().getDisplayName() +
                            " y no puede ser modificado");
            return "redirect:/inventarios/verInv";
        }

        // Cargar materiales con sus precios actuales
        List<Material> materialesConPrecios = materialServicio.listarTodosConPrecios();

        // Crear un mapa de materialId -> precioActual para acceso rápido en la vista
        Map<Long, BigDecimal> preciosActualesMap = new HashMap<>();
        for (Material material : materialesConPrecios) {
            preciosActualesMap.put(material.getIdMaterial(), material.getPrecioActual());
        }

        model.addAttribute("inventario", inventario);

        List<Inventario> inventarios = inventarioServicio.listaInventarios();
        model.addAttribute("inventarios", inventarios);

        model.addAttribute("preciosActualesMap", preciosActualesMap);

        model.addAttribute("obras", obtenerObrasDisponibles(usuarioLogeado));

        model.addAttribute("materiales", materialesConPrecios);

        model.addAttribute("precios", precioMaterialDao.findAll());

        model.addAttribute("estados", EstadoInventario.values());

        model.addAttribute("esAdmin", esAdmin(usuarioLogeado));

        model.addAttribute("esEdicion", true);

        return "inventarios/cambiarInv";
    }


    // ========== ELIMINAR (ANULAR) INVENTARIO ==========
    @GetMapping("/borrarInv")
    public String borrarInv(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
            Model model) {

        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

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

        // Filtrar solo los que se pueden anular (no finales)
        inventarios = inventarios.stream()
                .filter(i -> !i.isEstadoFinal())
                .collect(Collectors.toList());

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("usuarioActual", usuarioLogeado);
        return "inventarios/borrarInv";
    }


    @GetMapping("/borrarInv/{id_Inventario}")
    public String borrarInventario(
            @PathVariable("id_Inventario") Long id,
            @RequestParam(value = "motivo", required = false) String motivo,
            RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);
        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);

        if (inventario == null) {
            redirectAttributes.addFlashAttribute("error", "Inventario no encontrado");
            return "redirect:/inventarios";
        }

        if (inventario.isEstadoFinal()) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede anular un inventario en estado " + inventario.getAprobacion().getDisplayName());
            return "redirect:/inventarios";
        }

        try {
            // Anular el inventario (cambiar a estado ANULADO)
            inventarioServicio.cambiarEstado(id, EstadoInventario.ANULADO, usuarioLogeado,
                    "Anulado por usuario: " + (motivo != null ? motivo : "Sin motivo especificado"),request.getRemoteAddr(),request.getHeader("User-Agent"));

            redirectAttributes.addFlashAttribute("success",
                    "Inventario #" + id + " anulado exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al anular: " + e.getMessage());
        }
        return "redirect:/inventarios";
    }

    // ========== EXPORTAR EXCEL ==========
    //Exportar excel de inventario
    @GetMapping("/exportarExcelInv")
    public void exportarExcelInv(@PathVariable("idInventario") Long id,HttpServletResponse response) throws IOException {


        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        if (inventario == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String nombreObra = inventario.getIdObra() != null ?
                inventario.getIdObra().getNombreObra().replaceAll("[^a-zA-Z0-9]", "_") :
                "sin_obra";
        String nombreArchivo = nombreObra + "_" + id + "_" +
                LocalDate.now().format(formatter) + ".xlsx";



        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Inventarios");

        Row header = hoja.createRow(0);
        header.createCell(0).setCellValue("ID Inventario");
        header.createCell(1).setCellValue("Estado");
        header.createCell(2).setCellValue("Gestor");
        header.createCell(3).setCellValue("Obra");
        header.createCell(4).setCellValue("Fecha Solicitud");
        header.createCell(5).setCellValue("Fecha Entrega");
        header.createCell(6).setCellValue("Material");
        header.createCell(7).setCellValue("Cantidad");
        header.createCell(8).setCellValue("Unidad");

        int fila = 1;
        for (MaterialesInventario mat : inventario.getMaterialesInventarios()){
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(inventario.getIdInventario());
            row.createCell(1).setCellValue(inventario.getAprobacion().getDisplayName());
            row.createCell(2).setCellValue(inventario.getIdUsuario() != null ? inventario.getIdUsuario().getNombreUsuario() : "N/A");
            row.createCell(3).setCellValue(inventario.getIdObra() != null ? inventario.getIdObra().getNombreObra() : "N/A");
            row.createCell(4).setCellValue(inventario.getFechaIngreso() != null ? inventario.getFechaIngreso().format(formatter) : "N/A");
            row.createCell(5).setCellValue(inventario.getFechaEntrega() != null ? inventario.getFechaEntrega().format(formatter) : "N/A");
            row.createCell(6).setCellValue(mat.getMaterial() != null ? mat.getMaterial().getNombreMaterial() : "N/A");
            row.createCell(7).setCellValue(mat.getCantidad());
            row.createCell(8).setCellValue(mat.getMaterial() != null ? mat.getMaterial().getUnidadMaterial() : "N/A");
        }
        libro.write(response.getOutputStream());
        libro.close();
    }

    // ========== RESÚMENES Y ESTADÍSTICAS ==========
    @GetMapping("/estadisticas")
    public String verEstadisticas(Model model) {
        // Obtener el usuario actualmente autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuarioLogeado = usuarioServicio.encontrarPorNombreUsuario(username);

        // Estadísticas generales
        Map<EstadoInventario, Long> conteoPorEstado = new HashMap<>();
        Map<String, Long> conteoPorObra = new HashMap<>();
        Map<String, Long> conteoPorUsuario = new HashMap<>();

        List<Inventario> inventarios = obtenerInventariosVisibles(usuarioLogeado);

        // Conteo por estado
        for (EstadoInventario estado : EstadoInventario.values()) {
            long count = inventarios.stream()
                    .filter(i -> i.getAprobacion() == estado)
                    .count();
            conteoPorEstado.put(estado, count);
        }

        // Conteo por obra
        Map<Long, Long> obraCount = inventarios.stream()
                .filter(i -> i.getIdObra() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getIdObra().getIdObra(),
                        Collectors.counting()
                ));

        // Conteo por usuario
        Map<Long, Long> usuarioCount = inventarios.stream()
                .filter(i -> i.getIdUsuario() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getIdUsuario().getIdUsuario(),
                        Collectors.counting()
                ));

        model.addAttribute("conteoPorEstado", conteoPorEstado);
        model.addAttribute("totalInventarios", inventarios.size());
        model.addAttribute("inventariosPendientes",
                inventarios.stream().filter(i -> i.getAprobacion() == EstadoInventario.SOLICITADO).count());
        model.addAttribute("inventariosActivos",
                inventarios.stream().filter(i -> !i.isEstadoFinal()).count());
        model.addAttribute("esAdmin", esAdmin(usuarioLogeado));

        return "inventarios/estadisticas";
    }



    // ========== MÉTODOS AUXILIARES ==========

    private Usuario getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return usuarioServicio.encontrarPorNombreUsuario(username);
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.getRol() != null &&
                "ADMIN".equals(usuario.getRol().getNombreRol());
    }

    private boolean puedeCrearInventario(Usuario usuario) {
        if (usuario == null) return false;
        if (esAdmin(usuario)) return true;
        return usuario.getEquipo() != null;
    }

    private boolean tieneAccesoAlInventario(Usuario usuario, Inventario inventario) {
        if (usuario == null || inventario == null) return false;
        if (esAdmin(usuario)) return true;

        // Es su propio inventario
        if (inventario.getIdUsuario() != null &&
                inventario.getIdUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            return true;
        }

        // Es de una obra de su equipo
        if (inventario.getIdObra() != null) {
            Set<Long> obrasPermitidas = obtenerObrasPermitidas(usuario);
            return obrasPermitidas.contains(inventario.getIdObra().getIdObra());
        }

        return false;
    }

    private boolean tieneAccesoAObra(Usuario usuario, Obra obra) {
        if (usuario == null || obra == null) return false;
        if (esAdmin(usuario)) return true;

        Set<Long> obrasPermitidas = obtenerObrasPermitidas(usuario);
        return obrasPermitidas.contains(obra.getIdObra());
    }

    private Set<Long> obtenerObrasPermitidas(Usuario usuario) {
        Set<Long> obrasPermitidas = new HashSet<>();

        // Si tiene equipo, incluir todas las obras de los proyectos del equipo
        if (usuario.getEquipo() != null) {
            List<Obra> obras = obraServicio.listaObra();
            for (Obra obra : obras) {
                if (obra.getProyecto() != null &&
                        obra.getProyecto().getEquipo() != null &&
                        obra.getProyecto().getEquipo().getIdEquipo().equals(usuario.getEquipo().getIdEquipo())) {
                    obrasPermitidas.add(obra.getIdObra());
                }
            }
        }

        return obrasPermitidas;
    }

    private List<Obra> obtenerObrasDisponibles(Usuario usuario) {
        if (esAdmin(usuario)) {
            return obraServicio.listaObra();
        }

        List<Obra> obrasDisponibles = new ArrayList<>();
        Set<Long> obrasPermitidas = obtenerObrasPermitidas(usuario);

        for (Obra obra : obraServicio.listaObra()) {
            if (obrasPermitidas.contains(obra.getIdObra())) {
                obrasDisponibles.add(obra);
            }
        }

        return obrasDisponibles;
    }

    private List<Inventario> obtenerInventariosVisibles(Usuario usuario) {
        List<Inventario> todos = inventarioServicio.listaInventarios();

        if (esAdmin(usuario)) {
            return todos;
        }

        Set<Long> obrasPermitidas = obtenerObrasPermitidas(usuario);

        return todos.stream()
                .filter(i -> i.getIdUsuario() != null &&
                        (i.getIdUsuario().getIdUsuario().equals(usuario.getIdUsuario()) ||
                                (i.getIdObra() != null && obrasPermitidas.contains(i.getIdObra().getIdObra()))))
                .collect(Collectors.toList());
    }
}
