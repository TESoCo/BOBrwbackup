package com.example.servicio;

import com.example.dao.*;
import com.example.domain.*;
import com.example.domain.enums.EstadoInventario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class InventarioServicioImp implements InventarioServicio {

    @Autowired
    private InventarioDao inventarioDao;

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private ObraDao obraDao;

    @Autowired
    private MaterialServicio materialServicio;
    @Autowired
    private AuditoriaDao auditoriaDao;

    @Autowired
    private HttpServletRequest request;

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> listaInventarios() {
        return (List<Inventario>) inventarioDao.findAll();
    }

    @Override
    @Transactional
    public void guardarInv(Inventario inventario) {
        inventarioDao.save(inventario);
    }

    @Override
    @Transactional
    public void cambiarInv(Inventario inventario) {
        inventarioDao.save(inventario);
    }

    @Override
    @Transactional
    public void borrarInv(Inventario inventario) {
        inventarioDao.delete(inventario);
    }

    @Override
    @Transactional(readOnly = true)
    public Inventario localizarInventarioPorId(Long id) {
        return inventarioDao.findById(Long.valueOf(id)).orElse(null);
    }

    // Métodos de búsqueda implementados
    @Override
    @Transactional(readOnly = true)
    public List<Inventario> buscarPorNombreGestor(String nombreGestor) {
        try {
            Usuario usuario = usuarioDao.findByNombreUsuario(nombreGestor);
            // Verificar si el usuario existe
            if (usuario == null || usuario.getIdUsuario() == null) {
                return Collections.emptyList(); // Retorna lista vacía si no existe
            }
            return inventarioDao.findByIdUsuario_idUsuario (usuario.getIdUsuario());
        } catch (Exception e) {

            return Collections.emptyList();
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<Inventario> buscarPorNombreObra(String nombreObra) {
        return obraDao.findByNombreObra(nombreObra).stream()
                .findFirst()
                .map(obra -> inventarioDao.findByIdObra(obra))
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> buscarPorFecha(String fecha) {
        try {
            // Convertir String a LocalDate
            LocalDate fechaBusqueda = LocalDate.parse(fecha);
            return inventarioDao.findByFechaIngreso(fechaBusqueda);
        } catch (DateTimeParseException e) {
            System.err.println("Formato de fecha inválido: " + fecha);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public void agregarMaterialAInvConCantidad(Inventario inventario, Material material, Double cantidad) {
        MaterialesInventario materialesInventario = new MaterialesInventario();
        materialesInventario.setInventario(inventario);
        materialesInventario.setMaterial(material);
        materialesInventario.setCantidad(cantidad);
        inventario.getMaterialesInventarios().add(materialesInventario);
    }

    // ========== MÉTOD PRINCIPAL DE CAMBIO DE ESTADO ==========
    @Override
    @Transactional
    public Inventario cambiarEstado(Long idInventario, EstadoInventario nuevoEstado,
                                    Usuario usuario, String comentario,
                                    String ipOrigen, String userAgent) {

        // 1. Validar que el inventario existe
        Inventario inventario = inventarioDao.findById(idInventario)
                .orElseThrow(() -> new IllegalArgumentException("Inventario no encontrado"));

        // 2. Validar transición
        EstadoInventario estadoActual = inventario.getAprobacion();

        if (!estadoActual.isTransicionValida(nuevoEstado)) {
            throw new IllegalStateException(
                    String.format("No se puede pasar de %s a %s",
                            estadoActual.getDisplayName(),
                            nuevoEstado.getDisplayName())
            );
        }

        // 3. Validar permisos según el nuevo estado
        validarPermisosCambioEstado(estadoActual, nuevoEstado, usuario, inventario);

        // 4. Guardar estado anterior para auditoría
        String estadoAnterior = inventario.getAprobacion().getCodigo();

        // 5. Ejecutar lógica de negocio según el nuevo estado
        ejecutarLogicaTransicion(inventario, estadoActual, nuevoEstado, usuario);

        // 6. Si es ENTREGADO, registrar fecha
        if (nuevoEstado == EstadoInventario.ENTREGADO) {
            inventario.setFechaEntrega(LocalDate.now());
        }

        // 7. Cambiar estado
        inventario.setAprobacion(nuevoEstado);

        // 8. Guardar inventario
        inventario = inventarioDao.save(inventario);

        // 9. Registrar auditoría
        registrarAuditoria(inventario, estadoAnterior, nuevoEstado.name(), usuario, comentario);

        // 10. Si se proporcionaron IP y UserAgent, actualizar la auditoría
        if (ipOrigen != null || userAgent != null) {
            List<Auditoria> auditorias = auditoriaDao.findByEntidadAndIdEntidadOrderByFechaCambioDesc("INVENTARIO", inventario.getIdInventario());
            if (!auditorias.isEmpty()) {
                Auditoria ultima = auditorias.get(0);
                if (ipOrigen != null) ultima.setIpOrigen(ipOrigen);
                if (userAgent != null) ultima.setUserAgent(userAgent);
                auditoriaDao.save(ultima);
            }
        }

        return inventario;
    }



    // Constantes para roles
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_SUPERVISOR = "SUPERVISOR";
    private static final String ROL_OPERATIVO = "OPERATIVO";

    private void validarPermisosCambioEstado(EstadoInventario estadoActual,
                                             EstadoInventario nuevoEstado,
                                             Usuario usuario,Inventario inventario) {

        if (usuario == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        String rolUsuario = usuario.getRol() != null ? usuario.getRol().getNombreRol() : "";



        switch (nuevoEstado) {
            case APROBADO:
                // Solo ADMIN o SUPERVISOR pueden aprobar
                if (!ROL_ADMIN.equals(rolUsuario) && !ROL_SUPERVISOR.equals(rolUsuario)) {
                    throw new AccessDeniedException(
                            "Solo administradores o supervisores pueden aprobar inventario. Rol actual: " + rolUsuario
                    );
                }
                break;

            case ENTREGADO:
                // ADMIN, SUPERVISOR o el mismo usuario que creó la solicitud (si es OPERATIVO)
                if (!ROL_ADMIN.equals(rolUsuario) && !ROL_SUPERVISOR.equals(rolUsuario)) {
                    // Si es OPERATIVO, verificar que sea el creador
                    if (!ROL_OPERATIVO.equals(rolUsuario) ||
                            !usuario.getIdUsuario().equals(inventario.getIdUsuario().getIdUsuario())) {
                        throw new AccessDeniedException(
                                "Solo el creador de la solicitud (si es operativo), supervisores o administradores pueden entregar"
                        );
                    }
                }
                break;

            case RECHAZADO:
                // ADMIN o SUPERVISOR pueden rechazar
                if (!ROL_ADMIN.equals(rolUsuario) && !ROL_SUPERVISOR.equals(rolUsuario)) {
                    throw new AccessDeniedException(
                            "Solo administradores o supervisores pueden rechazar solicitudes"
                    );
                }
                break;

            case ANULADO:
                // Solo ADMIN o el creador pueden anular
                if (!ROL_ADMIN.equals(rolUsuario) &&
                        !usuario.getIdUsuario().equals(inventario.getIdUsuario().getIdUsuario())) {
                    throw new AccessDeniedException(
                            "Solo el creador o un administrador pueden anular la solicitud"
                    );
                }
                break;

            default:
                break;
        }
    }

    private void ejecutarLogicaTransicion(Inventario inventario, EstadoInventario estadoActual, EstadoInventario nuevoEstado, Usuario usuario) {

        switch (nuevoEstado) {
            case APROBADO:
                // Al aprobar, reservar stock
                // Solo reservar stock para OBRA
                if (inventario.getTipoDestino() == Inventario.TipoDestino.OBRA) {
                    reservarStock(inventario);
                }
                break;

            case ENTREGADO:
                // Al entregar, confirmar consumo y actualizar stock de obra
                confirmarEntregaStock(inventario);
                break;

            case RECHAZADO:
                // Al rechazar, liberar reserva si estaba aprobado
                if (estadoActual == EstadoInventario.APROBADO) {
                    liberarReservaStock(inventario);
                }
                break;

            case ANULADO:
                // Al anular, liberar stock si estaba aprobado
                if (estadoActual == EstadoInventario.APROBADO) {
                    liberarReservaStock(inventario);
                }
                break;

            default:
                break;
        }
    }

    @Override
    @Transactional
    public void reservarStock(Inventario inventario) {

        // Solo reservar stock si es tipo OBRA
        if (inventario.getTipoDestino() != Inventario.TipoDestino.OBRA) {
            return; // No reservar para STOCK
        }

        // Obtener todos los materiales del inventario
        List<MaterialesInventario> materiales = inventario.getMaterialesInventarios();

        for (MaterialesInventario mi : materiales) {
            Material material = mi.getMaterial();
            Double cantidad = mi.getCantidad();

            // Verificar que hay stock disponible
            if (material.getStockDisponible() < cantidad) {
                throw new IllegalStateException(
                        String.format("Stock insuficiente para el material %s. Disponible: %.2f, Solicitado: %.2f",
                                material.getNombreMaterial(),
                                material.getStockDisponible(),
                                cantidad)
                );
            }

            // Reservar stock (descontar del disponible)
            material.setStockDisponible(material.getStockDisponible() - cantidad);
            material.setStockReservado(material.getStockReservado() + cantidad);
            materialServicio.guardar(material);
        }
    }

    @Override
    @Transactional
    public void confirmarEntregaStock(Inventario inventario) {
        // Obtener todos los materiales del inventario
        List<MaterialesInventario> materiales = inventario.getMaterialesInventarios();

        // Determinar el tipo de operación según el tipo de destino
        boolean esStock = inventario.getTipoDestino() == Inventario.TipoDestino.STOCK;

        for (MaterialesInventario mi : materiales) {
            Material material = mi.getMaterial();
            Double cantidad = mi.getCantidad();

            if (esStock) {
                // ========== TIPO STOCK: AUMENTAR stock ==========
                // Aumentar el stock disponible
                material.setStockDisponible(material.getStockDisponible() + cantidad);

                // Si hay stock reservado, reducirlo (por si se había reservado)
                if (material.getStockReservado() >= cantidad) {
                    material.setStockReservado(material.getStockReservado() - cantidad);
                } else {
                    material.setStockReservado(0.0);
                }

            } else {
                // ========== TIPO OBRA: DISMINUIR stock ==========
                // Validar que hay suficiente stock TOTAL (disponible + reservado)
                double stockTotal = material.getStockDisponible() + material.getStockReservado();

                if (stockTotal < cantidad) {
                    throw new IllegalStateException(
                            String.format("Stock insuficiente para el material %s. " +
                                            "Disponible: %.2f, Reservado: %.2f, " +
                                            "Solicitado: %.2f",
                                    material.getNombreMaterial(),
                                    material.getStockDisponible(),
                                    material.getStockReservado(),
                                    cantidad)
                    );
                }

                // Reducir el stock disponible (priorizando usar el reservado primero)
                if (material.getStockReservado() >= cantidad) {
                    // Si hay suficiente reservado, usar solo reservado
                    material.setStockReservado(material.getStockReservado() - cantidad);
                } else {
                    // Si no alcanza el reservado, usar primero el reservado y luego el disponible
                    double cantidadRestante = cantidad - material.getStockReservado();
                    material.setStockReservado(0.0);
                    material.setStockDisponible(material.getStockDisponible() - cantidadRestante);

                    // Validación extra por si queda negativo
                    if (material.getStockDisponible() < 0) {
                        throw new IllegalStateException(
                                String.format("El stock disponible quedaría negativo para el material %s",
                                        material.getNombreMaterial())
                        );
                    }
                }
            }

            // Guardar los cambios en el material
            materialServicio.guardar(material);
        }
    }

    @Override
    @Transactional
    public void liberarReservaStock(Inventario inventario) {

        // Solo liberar reserva si es tipo OBRA
        if (inventario.getTipoDestino() != Inventario.TipoDestino.OBRA) {
            return; // No hay reserva que liberar para STOCK
        }

        List<MaterialesInventario> materiales = inventario.getMaterialesInventarios();

        for (MaterialesInventario mi : materiales) {
            Material material = mi.getMaterial();
            Double cantidad = mi.getCantidad();

            // Liberar reserva
            material.setStockReservado(Math.max(0, material.getStockReservado() - cantidad));
            material.setStockDisponible(material.getStockDisponible() + cantidad);
            materialServicio.guardar(material);
        }
    }

    @Override
    @Transactional
    public void registrarAuditoria(Inventario inventario, String estadoAnterior, String estadoNuevo, Usuario usuario, String comentario) {

        Auditoria auditoria = Auditoria.crear(
                "INVENTARIO",
                inventario.getIdInventario(),
                Auditoria.AccionAuditoria.STATUS_CHANGE,
                usuario
        );

        auditoria.setCampo("estado");
        auditoria.setValorAnterior(estadoAnterior);
        auditoria.setValorNuevo(estadoNuevo);
        auditoria.setComentario(comentario);

        // Capturar información de la solicitud
        if (request != null) {
            auditoria.setIpOrigen(request.getRemoteAddr());
            auditoria.setUserAgent(request.getHeader("User-Agent"));
        }

        auditoriaDao.save(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auditoria> obtenerAuditoriaPorInventario(Long idInventario) {
        Inventario inventario = localizarInventarioPorId(idInventario);
        return auditoriaDao.findByEntidadAndIdEntidadOrderByFechaCambioDesc("INVENTARIO", inventario.getIdInventario());
    }

    // Métod para obtener los estados permitidos para un usuario
    @Override
    @Transactional(readOnly = true)
    public List<EstadoInventario> obtenerEstadosPermitidos(Long idInventario, Usuario usuario) {
        Inventario inventario = localizarInventarioPorId(idInventario);
        if (inventario == null) {
            return List.of();
        }

        EstadoInventario estadoActual = inventario.getAprobacion();
        List<EstadoInventario> estadosPermitidos = new ArrayList<>();

        String rolUsuario = usuario.getRol() != null ? usuario.getRol().getNombreRol() : "";

        // Obtener todas las transiciones válidas
        for (EstadoInventario estado : EstadoInventario.values()) {
            if (estadoActual.isTransicionValida(estado)) {
                try {
                    // Verificar si el usuario tiene permisos para este estado
                    validarPermisosCambioEstado(estadoActual, estado, usuario, inventario);
                    estadosPermitidos.add(estado);
                } catch (AccessDeniedException e) {
                    // Si no tiene permisos, continuar con el siguiente
                }
            }
        }

        return estadosPermitidos;
    }





    // Filtrado por estado
    @Override
    @Transactional(readOnly = true)
    public List<Inventario> obtenerInventariosAprobados() {
        return inventarioDao.findByAprobacion(EstadoInventario.APROBADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> obtenerInventariosEntregados() {
        return inventarioDao.findByAprobacion(EstadoInventario.ENTREGADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> obtenerInventariosPendientes() {
        return inventarioDao.findByAprobacion(EstadoInventario.SOLICITADO);
    }

    public List<Inventario> buscarPorAprobacion(EstadoInventario estadoInventario) {
        return inventarioDao.findByAprobacion(estadoInventario);
    }

}