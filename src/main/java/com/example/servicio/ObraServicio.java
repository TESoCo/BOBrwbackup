package com.example.servicio;

import com.example.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ObraServicio {

    public List<Obra> listaObra();

    public void salvar(Obra obraGuardar);

    void actualizar(Obra obraActualizar);

    void actualizarActividadesDeObra(Long idObra, List<Long> actividadIds, List<Double> cantidades);

    public void borrar(Obra obraBorrar);

    // Localizar obra
    Obra localizarObra(Long entryId);

    Obra localizarObraConApus(Long idObra);

    // Métodos de búsqueda
    List<Obra> findByObraName(String obraName);
    List<Obra> findByObraNameContaining(String obraName);
    List<Obra> findByObraNameIgnoreCase(String obraName);

    // Métodos para APUs
    public List<Apu> listarApus();

    void agregarApuAObra(Obra obra, Apu apu);
    void agregarApuAObraConCantidad(Obra obra, Apu apu, Double cantObra);
    Map<Long, Double> obtenerApusPorObra(Long idObra);
    List<Apu> obtenerApusEntidadesPorObra(Long idObra);

   //Duración calculada con APUs
   void calcularDuracionLinealObra(Obra obra);

    // para filtrar obras
    List<Obra> findByProyectoIsNull();
    List<Obra> findByProyectoIdProyecto(Long idProyecto);

    // funcionalidad por etapas
    Obra getObraPresupuesto(Long idProyecto);
    Obra getObraEjecucion(Long idProyecto);
    Obra getObraCierre(Long idProyecto);
    List<ApusObra> getApusPresupuesto(Long idProyecto);
    List<ApusObra> getApusEjecucion(Long idProyecto);
    List<ApusObra> getApusCierre(Long idProyecto);
    boolean isPresupuestoCompletado(Long idProyecto);
    boolean isEjecucionActiva(Long idProyecto);
    Proyecto encontrarPorIdentificadorObra(String identificadorUnico);

    // NUEVOS MÉTODOS PARA EL FLUJO DE ETAPAS
    // 1. Crear obra en PRESUPUESTO
    Obra crearObraPresupuesto(String nombreObra, LocalDate fechaIni, LocalDate fechaFin,
                              Double cooNObra, Double cooEObra,
                              Map<Long, Double> actividadesCantidades,
                              Long idProyecto, Usuario usuarioCreador) ;

    // 2. Avanzar de PRESUPUESTO a EJECUCIÓN
    Obra avanzarAEjecucion(Long idObraPresupuesto, LocalDate fechaInicioReal, Usuario usuario);

    // 3. Avanzar de EJECUCIÓN a CIERRE
    Obra avanzarACierre(Long idObraEjecucion, LocalDate fechaCierreReal, Usuario usuario);

    // 4. Obtener presupuesto de una obra
    Obra obtenerPresupuestoDeObra(Long idObra);

    // 5. Validaciones
    boolean puedeAvanzarAEjecucion(Long idObra);
    boolean puedeAvanzarACierre(Long idObra);

    // 6. Cálculo de porcentaje de avance
    Double calcularPorcentajeAvance(Long idObraEjecucion);

    // 7. Comparativa
    Map<String, Object> obtenerComparativa(Long idObra);

    // 8. Métodos de comparación de ApusObra
    Double getPorcentajeAvance(ApusObra apusEjecucion, Obra obraPresupuesto);
    Double getCantidadRestante(ApusObra apusEjecucion, Obra obraPresupuesto);

    // 9. Buscar obras por identificador
    List<Obra> findObrasByIdentificador(String identificadorUnico);



    //ACCESO POR EQUIPOS
    // OBTENER OBRAS VISIBLES PARA EL USUARIO
    public List<Obra> obtenerObrasVisibles(Usuario usuario);

    public boolean puedeCrearObra(Usuario usuario);

    public boolean proyectoPerteneceAlEquipoDeUsuario(Usuario usuario, Long idProyecto);

    public List<Proyecto> obtenerProyectosDisponibles(Usuario usuario);
}
