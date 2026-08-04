package com.example.servicio;

import com.example.domain.Auditoria;
import com.example.domain.Inventario;
import com.example.domain.Material;
import com.example.domain.Usuario;
import com.example.domain.enums.EstadoInventario;

import java.util.List;

public interface InventarioServicio {

    List<Inventario> listaInventarios();

    void guardarInv(Inventario inventario);

    void cambiarInv(Inventario inventario);

    void borrarInv(Inventario inventario);

    Inventario localizarInventarioPorId(Long id);

    //Metodo de busqueda para el de modificar inventario
    List<Inventario> buscarPorNombreGestor(String nombreGestor);
    List<Inventario> buscarPorNombreObra(String nombreObra);
    List<Inventario> buscarPorFecha(String fecha);


    public void agregarMaterialAInvConCantidad(Inventario inventario, Material material, Double cantidad);


    public Inventario cambiarEstado(Long idInventario, EstadoInventario nuevoEstado, Usuario usuario, String comentario, String ipOrigen, String userAgent);

    public void reservarStock(Inventario inventario);

    public void confirmarEntregaStock(Inventario inventario);

    public void liberarReservaStock(Inventario inventario);


    public void registrarAuditoria(Inventario inventario, String estadoAnterior, String estadoNuevo, Usuario usuario, String comentario);

    public List<Auditoria> obtenerAuditoriaPorInventario(Long idInventario);

    public List<EstadoInventario> obtenerEstadosPermitidos(Long idInventario, Usuario usuario);


    public List<Inventario> obtenerInventariosAprobados();

    public List<Inventario> obtenerInventariosEntregados();

    public List<Inventario> obtenerInventariosPendientes();

    public List<Inventario> buscarPorAprobacion(EstadoInventario estadoInventario);
}