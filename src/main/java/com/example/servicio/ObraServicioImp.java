package com.example.servicio;

import com.example.dao.*;
import com.example.domain.*;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ObraServicioImp implements ObraServicio {

    @Autowired
    private ObraDao obraDao;

    @Autowired
    private ApuDao apuDao;

    @Autowired
    private ApusObraDao apusObraDao;

    @Autowired
    private ProyectoDao proyectoDao;

    @Autowired
    private AvanceDao avanceDao;

    @Autowired
    private AuditoriaDao auditoriaDao;

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private EquipoServicio equipoServicio;

    @Autowired
    private PreciarioServicio preciarioServicio;

    @Autowired
    private EntityManager entityManager;


    @Override
    @Transactional(readOnly = true)
    public List<Obra> listaObra() {
        return (List<Obra>) obraDao.findAll();
    }

    @Override
    @Transactional
    public void salvar(Obra obraGuardar) {
        obraDao.save(obraGuardar);
    }

    @Override
    @Transactional
    public void borrar(Obra obraBorrar) {
        obraDao.delete(obraBorrar);
    }

    @Override
    @Transactional
    public void actualizar(Obra obraActualizar) {
        obraDao.save(obraActualizar);
    }

    @Override
    @Transactional
    public void actualizarActividadesDeObra(Long idObra, List<Long> actividadIds, List<Double> cantidades, Usuario usuario) {

        System.out.println("=== ACTUALIZANDO ACTIVIDADES EN SERVICIO ===");
        System.out.println("ID Obra: " + idObra);

        Obra obra = localizarObra(idObra);
        if (obra == null) {
            throw new RuntimeException("Obra no encontrada");
        }
        if (usuario == null) {
            throw new RuntimeException("Usuario no autenticado o no encontrado");
        }

        System.out.println("Usuario original: " + (obra.getIdUsuario() != null ? obra.getIdUsuario().getNombreUsuario() : "NULL"));

        // Guardar la fecha manual actual
        LocalDate fechaManualActual = obra.getFechaFinManual();

        // 1. LIMPIAR la sesión de Hibernate
        entityManager.flush();
        entityManager.clear();

        // 2. Recargar la obra para tener una instancia limpia
        obra = localizarObra(idObra);

        // 3. Limpiar lista de APUs
        obra.getApusObraList().clear();

        // 4. Agregar nuevos APUs
        for (int i = 0; i < actividadIds.size(); i++) {
            Apu apu = apuServicio.obtenerPorId(actividadIds.get(i));
            if (apu != null && cantidades.get(i) > 0) {
                ApusObra nuevoApu = new ApusObra();
                nuevoApu.setObra(obra);
                nuevoApu.setApu(apu);
                nuevoApu.setCantidad(cantidades.get(i));
                // IMPORTANTE: Crear el ID compuesto
                nuevoApu.setId(new ApusObraId(obra.getIdObra(), apu.getIdAPU()));
                obra.getApusObraList().add(nuevoApu);
            }
        }

        // 6. Restaurar la fecha manual
        obra.setFechaFinManual(fechaManualActual);

        // 7. Recalcular la fecha calculada
        calcularDuracionLinealObra(obra);

        // 8. Actualizar usuario
        obra.setIdUsuario(usuario);

        // 9. Guardar
        obraDao.save(obra);
        System.out.println("✅ Obra guardada exitosamente");
    }

    @Override
    @Transactional(readOnly = true)
    public Obra localizarObra(Long entryId) {
        return obraDao.findById(entryId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Obra localizarObraConApus(Long idObra) {
        Optional<Obra> obraOpt = obraDao.findByIdWithApus(idObra);
        if (obraOpt.isEmpty()) {
            throw new RuntimeException("Obra no encontrada con ID: " + idObra);
        }
        Obra obra = obraOpt.get();

        // Log para debugging
        System.out.println("=== OBRA CARGADA CON APUS ===");
        System.out.println("ID: " + obra.getIdObra());
        System.out.println("Nombre: " + obra.getNombreObra());
        System.out.println("Etapa: " + obra.getEtapa());
        System.out.println("Cantidad de APUs: " + (obra.getApusObraList() != null ? obra.getApusObraList().size() : 0));
        if (obra.getApusObraList() != null) {
            for (ApusObra ao : obra.getApusObraList()) {
                System.out.println("  - APU ID: " + ao.getApu().getIdAPU() +
                        ", Nombre: " + ao.getApu().getNombreAPU() +
                        ", Cantidad: " + ao.getCantidad());
            }
        }

        return obra;
    }

    public List<Obra>  findByObraName(String obraName) {
        return obraDao.findByNombreObra(obraName);
    }

    public List<Obra> findByObraNameContaining(String obraName) {
        return obraDao.findByNombreObraContaining(obraName);
    }

    public List<Obra> findByObraNameIgnoreCase(String obraName) {
        return obraDao.findByNombreObraIgnoreCase(obraName);
    }

    public List<Apu> listarApus() {
        return (List<Apu>) apuDao.findAll();
    }

    @Override
    @Transactional
    public void agregarApuAObra(Obra obra, Apu apu) {
        ApusObra apusObra = new ApusObra();
        apusObra.setObra(obra);
        apusObra.setApu(apu);
        apusObraDao.save(apusObra);
    }

    @Override
    @Transactional
    public void agregarApuAObraConCantidad(Obra obra, Apu apu, Double cantObra) {
        ApusObra apusObra = new ApusObra();
        apusObra.setObra(obra);
        apusObra.setApu(apu);
        apusObra.setCantidad(cantObra);
        apusObraDao.save(apusObra);

    }


    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> obtenerApusPorObra(Long idObra) {
        List<ApusObra> apusObra = apusObraDao.findByObra_IdObra(idObra);
        Map<Long, Double> result = new HashMap<>();
        for (ApusObra ao : apusObra) {
            result.put(ao.getApu().getIdAPU(), ao.getCantidad());
        }
        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Apu> obtenerApusEntidadesPorObra(Long idObra) {
        List<ApusObra> apusObra = apusObraDao.findByObra_IdObra(idObra);
        return apusObra.stream()
                .map(ApusObra::getApu)
                .toList();
    }

    @Override
    @Transactional
    public void calcularDuracionLinealObra(Obra obra) {
        try {
            System.out.println("=== CALCULANDO DURACIÓN ===");
            List<ApusObra> apusObraList = obra.getApusObraList();
            System.out.println("APUs en obra: " + (apusObraList != null ? apusObraList.size() : 0));
            BigDecimal duracionTotalObra = BigDecimal.ZERO;
            for(ApusObra apusObra : apusObraList) {
                Apu apu = apusObra.getApu();
                if (apu != null) {
                    BigDecimal duracion = apu.getDuracionAPU();
                    System.out.println("APU: " + apu.getNombreAPU() + ", Duración: " + duracion);

                    // ✅ Verificar que la duración no sea null
                    if (duracion != null) {
                        duracionTotalObra = duracionTotalObra.add(duracion);
                    }
                }
            }
            System.out.println("Duración total: " + duracionTotalObra);
            if(obra.getFechaIni()!=null){
                obra.setFechaFinCalculada(obra.getFechaIni().plusDays(duracionTotalObra.longValue()));
            }
        } catch (Exception e) {
            System.err.println("=== ERROR EN calcularDuracionLinealObra ===");
            e.printStackTrace();
            throw e;  // ← Re-lanza la excepción para que se vea
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> findByProyectoIsNull() {
        List<Obra> todasObras = (List<Obra>) obraDao.findAll();
        return todasObras.stream()
                .filter(obra -> obra.getProyecto() == null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> findByProyectoIdProyecto(Long idProyecto) {
        List<Obra> todasObras = (List<Obra>) obraDao.findAll();
        return todasObras.stream()
                .filter(obra -> obra.getProyecto() != null &&
                        obra.getProyecto().getIdProyecto().equals(idProyecto))
                .collect(Collectors.toList());
    }




//METODOS CON NUEVA ESTRUCTURA DE PROYECTOS



    @Override
    public Obra getObraPresupuesto(String idUnico) {
        List<Obra> obras = obraDao.findByIdentificadorUnico(idUnico);
        if (obras == null) return null;
        for (Obra obra : obras){
            if (obra.getEtapa() == Obra.EtapaObra.PRESUPUESTO){
                return obra;
            }
        }
        return null;
    }

    @Override
    public Obra getObraEjecucion(String idUnico) {
        List<Obra> obras = obraDao.findByIdentificadorUnico(idUnico);
        if (obras == null) return null;
        for (Obra obra : obras){
            if (obra.getEtapa() == Obra.EtapaObra.EJECUCION){
                return obra;
            }
        }
        return null;
    }

    @Override
    public Obra getObraCierre(String idUnico) {
        List<Obra> obras = obraDao.findByIdentificadorUnico(idUnico);
        if (obras == null) return null;
        for (Obra obra : obras){
            if (obra.getEtapa() == Obra.EtapaObra.CIERRE){
                return obra;
            }
        }
        return null;
    }

    @Override
    public List<ApusObra> getApusPresupuesto(String idUnico) {
        Obra presupuesto = getObraPresupuesto(idUnico);
        return presupuesto != null ? presupuesto.getApusObraList() : new ArrayList<>();
    }

    @Override
    public List<ApusObra> getApusEjecucion(String idUnico) {
        Obra ejecucion = getObraEjecucion(idUnico);
        return ejecucion != null ? ejecucion.getApusObraList() : new ArrayList<>();
    }

    @Override
    public List<ApusObra> getApusCierre(String idUnico) {
        Obra cierre = getObraCierre(idUnico);
        return cierre != null ? cierre.getApusObraList() : new ArrayList<>();
    }

    @Override
    public boolean isPresupuestoCompletado(String idUnico) {
        Obra presupuesto = getObraPresupuesto(idUnico);
        return presupuesto != null &&
                presupuesto.getApusObraList() != null &&
                !presupuesto.getApusObraList().isEmpty();
    }

    @Override
    public boolean isEjecucionActiva(Long idObra) {
        List<Obra> obras = obraDao.findById(idObra).stream().toList();
        if (obras == null) return false;
        for (Obra obra : obras){
            if (obra.getEtapa() == Obra.EtapaObra.EJECUCION){
                return true;
            }
        }
        return false;
    }



// ========== NUEVOS MÉTODOS PARA EL FLUJO DE ETAPAS ==========

    // CREAR OBRA CON VALIDACIONES
    @Override
    @Transactional
    public Obra crearObraPresupuesto(String nombreObra, LocalDate fechaIni, LocalDate fechaFin,
                                     Double cooNObra, Double cooEObra,
                                     Map<Long, Double> actividadesCantidades,
                                     Long idProyecto, Usuario usuarioCreador) {

        //Háblame cariño
        System.out.println("=== CREATING OBRA PRESUPUESTO ===");
        System.out.println("Nombre: " + nombreObra);
        System.out.println("Proyecto ID: " + idProyecto);
        System.out.println("Usuario: " + (usuarioCreador != null ? usuarioCreador.getNombreUsuario() : "null"));
        System.out.println("Actividades: " + actividadesCantidades.size());

        // 1. Validar que el usuario puede crear obras
        if (!puedeCrearObra(usuarioCreador)) {
            throw new RuntimeException("El usuario no tiene un equipo asignado. No puede crear obras.");
        }

        // 2. Validar que el proyecto es obligatorio
        if (idProyecto == null) {
            throw new RuntimeException("El proyecto es obligatorio para registrar una obra.");
        }

        // 3. Validar que el proyecto existe
        Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);
        if (proyecto == null) {
            throw new RuntimeException("El proyecto seleccionado no existe.");
        }

        // 4. Si no es ADMIN, validar que el proyecto pertenece a su equipo
        if (usuarioCreador.getRol() == null || !"ADMIN".equals(usuarioCreador.getRol().getNombreRol())) {
            if (!proyectoPerteneceAlEquipoDeUsuario(usuarioCreador, idProyecto)) {
                throw new RuntimeException("El proyecto seleccionado no pertenece a su equipo.");
            }
        }

        // Validar nombre único
        List<Obra> obrasExistentes = obraDao.findByNombreObra(nombreObra);
        if (!obrasExistentes.isEmpty()) {
            throw new RuntimeException("Ya existe una obra con el nombre: " + nombreObra);
        }

        // Generar identificador único
        //String identificadorUnico = UUID.randomUUID().toString();

        // Crear obra en PRESUPUESTO
        Obra obraPresupuesto = new Obra();
        obraPresupuesto.setNombreObra(nombreObra + " (PRESUPUESTO)");
        obraPresupuesto.setEtapa(Obra.EtapaObra.PRESUPUESTO);
        //obraPresupuesto.setIdentificadorUnico(identificadorUnico);
        obraPresupuesto.setFechaIni(fechaIni);
        obraPresupuesto.setFechaFinManual(fechaFin);
        obraPresupuesto.setCooNObra(cooNObra);
        obraPresupuesto.setCooEObra(cooEObra);
        obraPresupuesto.setAnular(false);
        obraPresupuesto.setIdUsuario(usuarioCreador);
        obraPresupuesto.setProyecto(proyecto);



        // Guardar obra
        obraDao.saveAndFlush(obraPresupuesto);
        System.out.println("=== AFTER SAVE ===");
        System.out.println("ID: " + obraPresupuesto.getIdObra());
        System.out.println("Is present in DB? " + obraDao.findById(obraPresupuesto.getIdObra()).isPresent());

        // Agregar APUs con cantidades
        for (Map.Entry<Long, Double> entry : actividadesCantidades.entrySet()) {
            Apu apu = apuServicio.obtenerPorId(entry.getKey());
            if (apu != null && entry.getValue() > 0) {
                agregarApuAObraConCantidad(obraPresupuesto, apu, entry.getValue());
            }
        }

        // Recalcular duración
        calcularDuracionLinealObra(obraPresupuesto);

        return localizarObra(obraPresupuesto.getIdObra());
    }

    @Override
    @Transactional
    public Obra avanzarAEjecucion(Long idObraPresupuesto, LocalDate fechaInicioReal, Usuario usuario) {
        Obra obraPresupuesto = localizarObraConApus(idObraPresupuesto);

        System.out.println("=== AVANZANDO A EJECUCIÓN ===");
        System.out.println("Obra encontrada: " + obraPresupuesto.getNombreObra());
        System.out.println("APUs en presupuesto: " + obraPresupuesto.getApusObraList().size());

        if (obraPresupuesto == null) {
            throw new RuntimeException("Obra de presupuesto no encontrada");
        }

        if (!Obra.EtapaObra.PRESUPUESTO.equals(obraPresupuesto.getEtapa())) {
            throw new RuntimeException("La obra no está en etapa PRESUPUESTO");
        }

        if (obraPresupuesto.getApusObraList() == null || obraPresupuesto.getApusObraList().isEmpty()) {
            throw new RuntimeException("El presupuesto no tiene actividades asociadas");
        }

        // Verificar si ya existe ejecución para esta obra
        String identificador = obraPresupuesto.getIdentificadorUnico();
        if (identificador != null && !identificador.isEmpty()) {
            Obra ejecucionExistente = obraDao.findByIdentificadorUnicoAndEtapa(
                    identificador,
                    Obra.EtapaObra.EJECUCION
            );

            if (ejecucionExistente != null && !ejecucionExistente.isAnular()) {
                throw new RuntimeException("Ya existe una obra en EJECUCIÓN para este identificador: " + identificador);
            }
        }

        //Obtener proyecto del presupuesto
        Proyecto proyecto = obraPresupuesto.getProyecto();

        // Crear obra en EJECUCIÓN
        Obra obraEjecucion = new Obra();
        obraEjecucion.setNombreObra(obraPresupuesto.getNombreObra().replace(" (PRESUPUESTO)", "") + " (EJECUCIÓN)");
        obraEjecucion.setEtapa(Obra.EtapaObra.EJECUCION);
        obraEjecucion.setIdentificadorUnico(obraPresupuesto.getIdentificadorUnico());
        obraEjecucion.setFechaIni(fechaInicioReal);
        obraEjecucion.setFechaFinManual(obraPresupuesto.getFechaFinManual());
        obraEjecucion.setCooNObra(obraPresupuesto.getCooNObra());
        obraEjecucion.setCooEObra(obraPresupuesto.getCooEObra());
        obraEjecucion.setAnular(false);
        obraEjecucion.setProyecto(proyecto);
        obraEjecucion.setIdUsuario(usuario);

        // Guardar obra de ejecución
        obraDao.save(obraEjecucion);
        System.out.println("Obra de ejecución guardada con ID: " + obraEjecucion.getIdObra());

        // Clonar estructura de APUs con cantidades en 0
        for (ApusObra apusPresupuesto : obraPresupuesto.getApusObraList()) {
            Apu apu = apusPresupuesto.getApu();
            obraEjecucion.agregarApu(apu, 0.0);
            System.out.println("APU agregado: " + apu.getNombreAPU() + " con cantidad 0");
        }

        // Guardar la obra con los APUs agregados
        obraEjecucion = obraDao.save(obraEjecucion);

        // Cerrar presupuesto
        obraPresupuesto.setAnular(true);
        obraDao.save(obraPresupuesto);

        return localizarObraConApus(obraEjecucion.getIdObra());
    }

    @Override
    @Transactional
    public Obra avanzarACierre(Long idObraEjecucion, LocalDate fechaCierreReal, Usuario usuario) {
        Obra obraEjecucion = localizarObra(idObraEjecucion);
        if (obraEjecucion == null) {
            throw new RuntimeException("Obra de ejecución no encontrada");
        }

        if (!Obra.EtapaObra.EJECUCION.equals(obraEjecucion.getEtapa())) {
            throw new RuntimeException("La obra no está en etapa EJECUCIÓN");
        }

        if (obraEjecucion.getApusObraList() == null || obraEjecucion.getApusObraList().isEmpty()) {
            throw new RuntimeException("La obra en ejecución no tiene actividades");
        }

        // Validar el presupuesto
        Obra presupuesto = obtenerPresupuestoDeObra(idObraEjecucion);
        if (presupuesto == null) {
            throw new RuntimeException("No se encontró el presupuesto asociado");
        }



        //Validar que no haya duplicados
        String identificador = obraEjecucion.getIdentificadorUnico();
        if (identificador != null && !identificador.isEmpty()) {
            Obra cierreExistente = obraDao.findByIdentificadorUnicoAndEtapa(
                    identificador,
                    Obra.EtapaObra.CIERRE
            );

            if (cierreExistente != null && !cierreExistente.isAnular()) {
                System.out.println("❌ Ya existe un CIERRE para el identificador: " + identificador);
                throw new RuntimeException("Ya existe un CIERRE para el identificador");
            }
        }

        // Verificar que todas las actividades estén al 100%
        // Crear un mapa de APU ID -> Cantidad presupuestada para fácil acceso
        Map<Long, Double> cantidadesPresupuestadas = new HashMap<>();
        for (ApusObra apusPresupuesto : presupuesto.getApusObraList()) {
            cantidadesPresupuestadas.put(apusPresupuesto.getApu().getIdAPU(), apusPresupuesto.getCantidad());
        }

        // Verificar cada APU en ejecución
        Map<String, Double> faltantes = new HashMap<>();
        for (ApusObra apusEjecucion : obraEjecucion.getApusObraList()) {
            Long apuId = apusEjecucion.getApu().getIdAPU();
            Double cantidadPresupuestada = cantidadesPresupuestadas.get(apuId);

            if (cantidadPresupuestada == null) {
                throw new RuntimeException("El APU " + apusEjecucion.getApu().getNombreAPU() +
                        " no existe en el presupuesto");
            }

            Double cantidadEjecutada = apusEjecucion.getCantidad() != null ? apusEjecucion.getCantidad() : 0.0;

            if (cantidadEjecutada < cantidadPresupuestada) {
                faltantes.put(apusEjecucion.getApu().getNombreAPU(),
                        cantidadPresupuestada - cantidadEjecutada);
            }
        }

        if (!faltantes.isEmpty()) {
            throw new RuntimeException("Actividades con cantidades faltantes: " + faltantes);
        }



        // Crear obra en CIERRE
        Obra obraCierre = new Obra();
        obraCierre.setNombreObra(obraEjecucion.getNombreObra().replace(" (EJECUCIÓN)", "") + " (CIERRE)");
        obraCierre.setEtapa(Obra.EtapaObra.CIERRE);
        obraCierre.setIdentificadorUnico(obraEjecucion.getIdentificadorUnico());
        obraCierre.setFechaIni(obraEjecucion.getFechaIni());
        obraCierre.setFechaFinManual(fechaCierreReal);
        obraCierre.setCooNObra(obraEjecucion.getCooNObra());
        obraCierre.setCooEObra(obraEjecucion.getCooEObra());
        obraCierre.setAnular(false);
        obraCierre.setProyecto(obraEjecucion.getProyecto());
        obraCierre.setIdUsuario(usuario);

        obraDao.save(obraCierre);
        System.out.println("Obra de cierre guardada con ID: " + obraCierre.getIdObra());

        // Copiar cantidades ejecutadas al cierre
        for (ApusObra apusEjecucion : obraEjecucion.getApusObraList()) {
            Apu apu = apusEjecucion.getApu();
            Double cantidad = apusEjecucion.getCantidad() != null ? apusEjecucion.getCantidad() : 0.0;
            obraCierre.agregarApu(apu, cantidad);
            System.out.println("APU agregado al cierre: " + apu.getNombreAPU() + " con cantidad " + cantidad);
        }

        // Guardar obra de cierre con APUs
        obraCierre = obraDao.save(obraCierre);

        // Cerrar ejecución
        obraEjecucion.setAnular(true);
        obraDao.save(obraEjecucion);

        return localizarObra(obraCierre.getIdObra());
    }

    @Override
    @Transactional(readOnly = true)
    public Obra obtenerPresupuestoDeObra(Long idObra) {
        try {
            // Obtener la obra actual
            Obra obraActual = localizarObra(idObra);
            if (obraActual == null) {
                System.out.println("No se encontró la obra con ID: " + idObra);
                return null;
            }

            // Buscar la obra PRESUPUESTO asociada por identificadorUnico
            String identificador = obraActual.getIdentificadorUnico();
            System.out.println("Buscando presupuesto con identificador: " + identificador);

            // CORREGIR: Usar el enum directamente, no un String
            return obraDao.findByIdentificadorUnicoAndEtapa(
                    identificador,
                    Obra.EtapaObra.PRESUPUESTO  // <- Esto es un enum, no un String
            );
        } catch (Exception e) {
            System.err.println("Error al obtener presupuesto para obra " + idObra + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    //Validaciones antes de avanzar etapa
    @Override
    @Transactional(readOnly = true)
    public boolean puedeAvanzarAEjecucion(Long idObra) {
        Obra obra = localizarObraConApus(idObra);

        System.out.println("=== VALIDANDO AVANCE A EJECUCIÓN ===");
        System.out.println("ID Obra: " + obra.getIdObra());
        System.out.println("Etapa: " + obra.getEtapa());
        System.out.println("APUs en obra: " + (obra.getApusObraList() != null ? obra.getApusObraList().size() : 0));

        if (!Obra.EtapaObra.PRESUPUESTO.equals(obra.getEtapa())) {
            System.out.println("❌ No está en PRESUPUESTO");
            return false;
        }

        if (obra.isAnular()) {
            System.out.println("❌ Está anulada");
            return false;
        }

        if (obra.getApusObraList() == null || obra.getApusObraList().isEmpty()) {
            System.out.println("❌ No tiene APUs asignados");
            return false;
        }

        String identificador = obra.getIdentificadorUnico();
        if (identificador != null && !identificador.isEmpty()) {
            Obra ejecucionExistente = obraDao.findByIdentificadorUnicoAndEtapa(identificador, Obra.EtapaObra.EJECUCION);
            if (ejecucionExistente != null && !ejecucionExistente.isAnular()) {
                System.out.println("❌ Ya existe una EJECUCIÓN para el identificador: " + identificador);
                System.out.println("   ID de ejecución existente: " + ejecucionExistente.getIdObra());
                return false;
            }
        }

        System.out.println("✅ Puede avanzar a EJECUCIÓN");
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeAvanzarACierre(Long idObra) {
        Obra obra = localizarObra(idObra);
        if (obra == null) return false;

        if (!Obra.EtapaObra.EJECUCION.equals(obra.getEtapa())) return false;
        if (obra.isAnular()) return false;
        if (obra.getApusObraList() == null || obra.getApusObraList().isEmpty()) return false;

        Obra presupuesto = obtenerPresupuestoDeObra(idObra);
        if (presupuesto == null) return false;

        for (ApusObra apusEjecucion : obra.getApusObraList()) {
            ApusObra apusPresupuesto = presupuesto.getApusObraList().stream()
                    .filter(ap -> ap.getApu().getIdAPU().equals(apusEjecucion.getApu().getIdAPU()))
                    .findFirst()
                    .orElse(null);

            if (apusPresupuesto == null) return false;
            if (apusEjecucion.getCantidad() < apusPresupuesto.getCantidad()) return false;
        }

        Proyecto proyecto = obra.getProyecto();
        if (proyecto != null) {
            return getObraCierre(obra.getIdentificadorUnico()) == null;
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calcularPorcentajeAvance(Long idObraEjecucion) {
        Obra ejecucion = localizarObra(idObraEjecucion);
        if (ejecucion == null || !Obra.EtapaObra.EJECUCION.equals(ejecucion.getEtapa())) {
            return 0.0;
        }

        Obra presupuesto = obtenerPresupuestoDeObra(idObraEjecucion);
        if (presupuesto == null) return 0.0;

        double totalPresupuestado = 0.0;
        double totalEjecutado = 0.0;

        for (ApusObra apusPresupuesto : presupuesto.getApusObraList()) {
            totalPresupuestado += apusPresupuesto.getCantidad();

            ApusObra apusEjecucion = ejecucion.getApusObraList().stream()
                    .filter(ap -> ap.getApu().getIdAPU().equals(apusPresupuesto.getApu().getIdAPU()))
                    .findFirst()
                    .orElse(null);

            totalEjecutado += apusEjecucion != null ? apusEjecucion.getCantidad() : 0.0;
        }

        if (totalPresupuestado == 0) return 0.0;
        return (totalEjecutado / totalPresupuestado) * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerComparativa(Long idObra) {
        Obra obra = localizarObra(idObra);
        if (obra == null) return new HashMap<>();

        Map<String, Object> comparativa = new HashMap<>();
        comparativa.put("obraActual", obra);

        Obra presupuesto = obtenerPresupuestoDeObra(idObra);
        comparativa.put("presupuesto", presupuesto);

        if (Obra.EtapaObra.EJECUCION.equals(obra.getEtapa())) {
            comparativa.put("porcentajeAvance", calcularPorcentajeAvance(idObra));

            List<Map<String, Object>> detalles = new ArrayList<>();
            for (ApusObra apusEjecucion : obra.getApusObraList()) {
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("apu", apusEjecucion.getApu());
                detalle.put("ejecutado", apusEjecucion.getCantidad());

                if (presupuesto != null) {
                    ApusObra apusPresupuesto = presupuesto.getApusObraList().stream()
                            .filter(ap -> ap.getApu().getIdAPU().equals(apusEjecucion.getApu().getIdAPU()))
                            .findFirst()
                            .orElse(null);
                    detalle.put("presupuestado", apusPresupuesto != null ? apusPresupuesto.getCantidad() : 0.0);
                    detalle.put("porcentaje", getPorcentajeAvance(apusEjecucion, presupuesto));
                    detalle.put("restante", getCantidadRestante(apusEjecucion, presupuesto));
                }
                detalles.add(detalle);
            }
            comparativa.put("detalles", detalles);
        }

        return comparativa;
    }

    // Métodos de comparación de ApusObra
    @Override
    @Transactional(readOnly = true)
    public Double getPorcentajeAvance(ApusObra apusEjecucion, Obra obraPresupuesto) {
        if (obraPresupuesto == null || apusEjecucion == null) return 0.0;

        ApusObra presupuesto = obraPresupuesto.getApusObraList().stream()
                .filter(ap -> ap.getApu().getIdAPU().equals(apusEjecucion.getApu().getIdAPU()))
                .findFirst()
                .orElse(null);

        if (presupuesto == null || presupuesto.getCantidad() == null || presupuesto.getCantidad() == 0) {
            return 0.0;
        }

        double cantidadEjecutada = apusEjecucion.getCantidad() != null ? apusEjecucion.getCantidad() : 0.0;
        return (cantidadEjecutada / presupuesto.getCantidad()) * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getCantidadRestante(ApusObra apusEjecucion, Obra obraPresupuesto) {
        if (obraPresupuesto == null || apusEjecucion == null) return 0.0;

        ApusObra presupuesto = obraPresupuesto.getApusObraList().stream()
                .filter(ap -> ap.getApu().getIdAPU().equals(apusEjecucion.getApu().getIdAPU()))
                .findFirst()
                .orElse(null);

        if (presupuesto == null || presupuesto.getCantidad() == null) {
            return 0.0;
        }

        double cantidadEjecutada = apusEjecucion.getCantidad() != null ? apusEjecucion.getCantidad() : 0.0;
        return presupuesto.getCantidad() - cantidadEjecutada;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> findObrasByIdentificador(String identificadorUnico) {
        return obraDao.findByIdentificadorUnico(identificadorUnico);
    }


    //ACCESO POR EQUIPOS
    // OBTENER OBRAS VISIBLES PARA EL USUARIO
    @Override
    @Transactional(readOnly = true)
    public List<Obra> obtenerObrasVisibles(Usuario usuario) {
        if (usuario == null) {
            return new ArrayList<>();
        }

        // Si es ADMIN, ver todas las obras
        if (usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombreRol())) {
            return obtenerObrasParaAdmin(usuario);
        }

        // Usuarios normales: solo obras de proyectos de su equipo + obras propias
        List<Obra> obrasVisibles = new ArrayList<>();

        // Obras propias
        obrasVisibles.addAll(obraDao.findByIdUsuario_IdUsuario(usuario.getIdUsuario()));

        // Obras de proyectos de su equipo
        if (usuario.getEquipo() != null) {
            List<Proyecto> proyectosDelEquipo = proyectoServicio.buscarPorEquipo(usuario.getEquipo().getIdEquipo());
            for (Proyecto proyecto : proyectosDelEquipo) {
                obrasVisibles.addAll(obraDao.findByProyecto_IdProyecto(proyecto.getIdProyecto()));
            }
        }

        return obrasVisibles.stream().distinct().collect(Collectors.toList());
    }

    private List<Obra> obtenerObrasParaAdmin(Usuario admin) {
        // Admin ve obras de equipos que creó

        List<Obra> obras = new ArrayList<>();

        // Obras propias del admin
        obras.addAll(obraDao.findByIdUsuario(admin));


        return obras.stream().distinct().collect(Collectors.toList());
    }

    // VERIFICAR SI USUARIO PUEDE CREAR OBRA
    @Override
    @Transactional(readOnly = true)
    public boolean puedeCrearObra(Usuario usuario) {
        if (usuario == null) return false;

        // Admin siempre puede crear
        if (usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombreRol())) {
            return true;
        }

        // Usuario normal necesita tener equipo
        return usuario.getEquipo() != null;
    }

    // VERIFICAR SI PROYECTO PERTENECE AL EQUIPO DEL USUARIO
    @Override
    @Transactional(readOnly = true)
    public boolean proyectoPerteneceAlEquipoDeUsuario(Usuario usuario, Long idProyecto) {
        if (usuario == null || usuario.getEquipo() == null) {
            return false;
        }

        Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);
        if (proyecto == null) {
            return false;
        }

        return proyecto.getEquipo() != null &&
                proyecto.getEquipo().getIdEquipo().equals(usuario.getEquipo().getIdEquipo());
    }


    @Override
    @Transactional(readOnly = true)
    public List<Proyecto> obtenerProyectosDisponibles(Usuario usuario) {

        if (usuario == null) {
            return new ArrayList<>();
        }

        // ADMIN: Ver TODOS los proyectos
        if (usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombreRol())) {
            return proyectoServicio.listarProyectos();
        }

        // Usuario normal: solo proyectos de su equipo
        if (usuario.getEquipo() != null) {
            return proyectoServicio.buscarPorEquipo(usuario.getEquipo().getIdEquipo());
        }

        return new ArrayList<>();
    }

    /**
     * Registrar auditoría para una obra
     */
    public void registrarAuditoria(Obra obra, String campo, String valorAnterior,
                                   String valorNuevo, Usuario usuario,
                                   String comentario, String ipOrigen, String userAgent) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setEntidad("OBRA");
            auditoria.setIdEntidad(obra.getIdObra());
            auditoria.setAccion(Auditoria.AccionAuditoria.UPDATE);
            auditoria.setCampo(campo);
            auditoria.setValorAnterior(valorAnterior);
            auditoria.setValorNuevo(valorNuevo);
            auditoria.setUsuario(usuario);
            auditoria.setComentario(comentario);
            auditoria.setIpOrigen(ipOrigen);
            auditoria.setUserAgent(userAgent);

            auditoriaDao.save(auditoria);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de obra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registrar auditoría de cambio de estado
     */
    public void registrarAuditoriaEstado(Obra obra, String estadoAnterior,
                                         String estadoNuevo, Usuario usuario,
                                         String ipOrigen, String userAgent) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setEntidad("OBRA");
            auditoria.setIdEntidad(obra.getIdObra());
            auditoria.setAccion(Auditoria.AccionAuditoria.STATUS_CHANGE);
            auditoria.setCampo("estado");
            auditoria.setValorAnterior(estadoAnterior);
            auditoria.setValorNuevo(estadoNuevo);
            auditoria.setUsuario(usuario);
            auditoria.setComentario("Cambio de estado de obra");
            auditoria.setIpOrigen(ipOrigen);
            auditoria.setUserAgent(userAgent);

            auditoriaDao.save(auditoria);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de estado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registrar auditoría de creación de obra
     */
    public void registrarAuditoriaCreacion(Obra obra, Usuario usuario,
                                           String ipOrigen, String userAgent) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setEntidad("OBRA");
            auditoria.setIdEntidad(obra.getIdObra());
            auditoria.setAccion(Auditoria.AccionAuditoria.INSERT);
            auditoria.setCampo("creacion");
            auditoria.setValorAnterior(null);
            auditoria.setValorNuevo("Obra creada: " + obra.getNombreObra());
            auditoria.setUsuario(usuario);
            auditoria.setComentario("Creación de nueva obra");
            auditoria.setIpOrigen(ipOrigen);
            auditoria.setUserAgent(userAgent);

            auditoriaDao.save(auditoria);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de creación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registrar auditoría de anulación de obra
     */
    public void registrarAuditoriaAnulacion(Obra obra, Usuario usuario,
                                            String motivo, String ipOrigen, String userAgent) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setEntidad("OBRA");
            auditoria.setIdEntidad(obra.getIdObra());
            auditoria.setAccion(Auditoria.AccionAuditoria.DELETE);
            auditoria.setCampo("anulacion");
            auditoria.setValorAnterior("Activa");
            auditoria.setValorNuevo("Anulada");
            auditoria.setUsuario(usuario);
            auditoria.setComentario("Obra anulada: " + motivo);
            auditoria.setIpOrigen(ipOrigen);
            auditoria.setUserAgent(userAgent);

            auditoriaDao.save(auditoria);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de anulación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtener auditoría por ID de obra
     */
    public List<Auditoria> obtenerAuditoriaPorObra(Long idObra) {
        return auditoriaDao.findByEntidadAndIdEntidadOrderByFechaCambioDesc("OBRA", idObra);
    }



    // IMPORTACIÓN DESDE EXEL
    @Override
    @Transactional
    public Obra importarObraDesdeExcel(MultipartFile archivo, Long idProyecto, Usuario usuario) throws IOException {
        System.out.println("Iniciando importación de obra desde Excel: " + archivo.getOriginalFilename());

        // Validar archivo
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo Excel está vacío");
        }

        String fileName = archivo.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("El archivo debe ser Excel (.xlsx o .xls)");
        }

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet hoja = workbook.getSheetAt(0);

            // Extraer datos
            ObraImportData data = extractObraData(hoja);

            // Validar proyecto
            Proyecto proyecto = proyectoServicio.encontrarPorId(idProyecto);
            if (proyecto == null) {
                throw new IllegalArgumentException("El proyecto con ID " + idProyecto + " no existe");
            }

            // Validar y obtener APUs por nombre
            Map<Long, Double> actividades = validateAndGetApusByName(data.getActividadesPorNombre());

            if (actividades.isEmpty()) {
                throw new IllegalArgumentException("No se encontraron APUs válidos en el archivo");
            }

            // Validar que los APUs pertenezcan al preciario del proyecto
            if (proyecto.getPreciario() != null) {
                List<Apu> apusPreciario = preciarioServicio.obtenerApusDePreciario(
                        proyecto.getPreciario().getIdPreciario());
                Set<Long> idsApusPermitidos = apusPreciario.stream()
                        .map(Apu::getIdAPU)
                        .collect(Collectors.toSet());

                List<String> apusNoPermitidos = new ArrayList<>();
                for (Long apuId : actividades.keySet()) {
                    if (!idsApusPermitidos.contains(apuId)) {
                        Apu apu = apuDao.findById(apuId).orElse(null);
                        if (apu != null) {
                            apusNoPermitidos.add(apu.getNombreAPU() + " (ID: " + apuId + ")");
                        }
                    }
                }

                if (!apusNoPermitidos.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Los siguientes APUs no pertenecen al preciario del proyecto '" +
                                    proyecto.getPreciario().getNombrePreciario() + "':\n" +
                                    String.join("\n", apusNoPermitidos)
                    );
                }
            }


            // Crear obra
            return crearObraPresupuesto(
                    data.getNombreObra(),
                    data.getFechaInicio(),
                    data.getFechaFin(),
                    data.getCooNObra(),
                    data.getCooEObra(),
                    actividades,
                    idProyecto,
                    usuario
            );
        } catch (Exception e) {
            System.err.println("Error al importar Excel: " + e.getMessage() + e);
            throw new IOException("Error al procesar el archivo Excel: " + e.getMessage(), e);
        }
    }

    private ObraImportData extractObraData(Sheet hoja) {
        // Título (fila 0)
        Row tituloRow = hoja.getRow(0);
        if (tituloRow == null) {
            throw new IllegalArgumentException("El archivo no tiene el formato esperado. Falta el título.");
        }

        String titulo = tituloRow.getCell(0).getStringCellValue();
        String nombreObra = titulo.replace("Actividades de la Obra: ", "").trim();

        // Información (fila 2)
        Row infoRow = hoja.getRow(2);
        if (infoRow == null) {
            throw new IllegalArgumentException("El archivo no tiene la fila de información.");
        }

        // Extraer fecha de inicio
        LocalDate fechaInicio = extractDateFromCell(infoRow.getCell(5));
        if (fechaInicio == null) {
            throw new IllegalArgumentException("No se pudo extraer la fecha de inicio");
        }

        // Extraer fecha de fin (si existe)
        LocalDate fechaFin = extractDateFromCell(infoRow.getCell(3));
        if (fechaFin == null) {
            fechaFin = fechaInicio.plusMonths(1);
        }

        // Extraer coordenadas
        Double[] coordenadas = extractCoordinates(infoRow.getCell(7));
        Double cooNObra = coordenadas[0];
        Double cooEObra = coordenadas[1];

        // Extraer APUs por nombre
        Map<String, Double> actividadesPorNombre = extractActivitiesByName(hoja);
        if (actividadesPorNombre.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene actividades válidas");
        }

        return new ObraImportData(
                nombreObra,
                fechaInicio,
                fechaFin,
                cooNObra,
                cooEObra,
                actividadesPorNombre
        );
    }

    /**
     * Extrae las actividades del Excel usando el nombre del APU (columna 1)
     */
    private Map<String, Double> extractActivitiesByName(Sheet hoja) {
        Map<String, Double> actividades = new LinkedHashMap<>();
        int rowIndex = 5; // Empieza después de los encabezados

        while (rowIndex <= hoja.getLastRowNum()) {
            Row row = hoja.getRow(rowIndex);
            if (row == null) {
                rowIndex++;
                continue;
            }

            // Verificar si es la fila de TOTAL
            if (isTotalRow(row)) {
                break;
            }

            try {
                // Obtener nombre del APU (columna 1)
                Cell nombreCell = row.getCell(1);
                if (nombreCell == null || nombreCell.getCellType() == CellType.BLANK) {
                    rowIndex++;
                    continue;
                }

                String nombreAPU = nombreCell.getStringCellValue().trim();
                if (nombreAPU.isEmpty()) {
                    rowIndex++;
                    continue;
                }

                // Obtener cantidad (columna 3)
                Cell cantidadCell = row.getCell(3);
                if (cantidadCell == null || cantidadCell.getCellType() == CellType.BLANK) {
                    rowIndex++;
                    continue;
                }

                Double cantidad = cantidadCell.getNumericCellValue();

                if (cantidad != null && cantidad > 0) {
                    actividades.put(nombreAPU, cantidad);
                    System.out.println("APU encontrado por nombre: " + nombreAPU +" cantidad: " + cantidad);
                }
            } catch (Exception e) {
                System.err.println("Error al leer fila: " + rowIndex + " " + e.getMessage());
            }
            rowIndex++;
        }

        return actividades;
    }

    /**
     * Normaliza un texto para búsqueda flexible:
     * - Convierte a mayúsculas
     * - Elimina espacios múltiples
     * - Elimina caracteres especiales
     * - Elimina acentos
     */
    private String normalizeText(String text) {
        if (text == null) return "";

        String normalized = text
                .toUpperCase()
                .replace("\"", "")           // Eliminar comillas dobles
                .replace("'", "")            // Eliminar comillas simples
                .replaceAll("[^A-Z0-9\\s]", " ")  // Solo letras, números y espacios
                .replaceAll("\\s+", " ")     // Espacios múltiples a uno
                .trim();

        return normalized;
    }

    /**
     * Extrae palabras clave significativas de un texto
     * (ignora palabras muy cortas o comunes)
     */
    private List<String> extractKeywords(String text) {
        String[] words = text.split("\\s+");
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "DE", "LA", "EL", "LOS", "LAS", "Y", "O", "PER",
                "EN", "CON", "SIN", "POR", "PARA", "UN", "UNA"
        ));

        return Arrays.stream(words)
                .filter(w -> w.length() > 2)  // Palabras de al menos 3 caracteres
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toList());
    }

    /**
     * Calcula el score de similitud entre dos textos
     */
    private double calculateSimilarity(String text1, String text2) {
        String norm1 = normalizeText(text1);
        String norm2 = normalizeText(text2);

        // Si son exactamente iguales, score máximo
        if (norm1.equals(norm2)) {
            return 1.0;
        }

        // Si uno contiene al otro
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            return 0.9;
        }

        // Comparar palabras clave
        List<String> keywords1 = extractKeywords(norm1);
        List<String> keywords2 = extractKeywords(norm2);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // Calcular coincidencia de palabras clave
        long matches = keywords1.stream()
                .filter(keyword -> keywords2.stream().anyMatch(k -> k.contains(keyword) || keyword.contains(k)))
                .count();

        double wordMatchScore = (double) matches / Math.max(keywords1.size(), keywords2.size());

        // Si hay al menos una coincidencia de palabra clave, dar un score base
        if (matches > 0) {
            return Math.max(0.3, wordMatchScore);
        }

        return 0.0;
    }

    /**
     * Busca APU por nombre con múltiples estrategias
     */
    private Apu findApuByFlexibleName(String nombreBuscado, List<String> errores, List<String> advertencias) {
        String nombreNormalizado = normalizeText(nombreBuscado);
        System.out.println("Buscando APU para: " + nombreBuscado + " (normalizado: " + nombreNormalizado + ")");

        // 1. ESTRATEGIA 1: Búsqueda exacta (MÁS IMPORTANTE)
        List<Apu> apusExactos = apuDao.findByNombreAPU(nombreBuscado);
        if (!apusExactos.isEmpty()) {
            System.out.println("Encontrado por nombre exacto: " + apusExactos.get(0).getNombreAPU());
            return apusExactos.get(0);
        }

        // 1b. Búsqueda exacta ignorando comillas y espacios
        List<Apu> todosApus = apuDao.findAll();
        for (Apu apu : todosApus) {
            String nombreApuNormalizado = normalizeText(apu.getNombreAPU());
            if (nombreApuNormalizado.equals(nombreNormalizado)) {
                System.out.println("Encontrado por nombre normalizado: " + apu.getNombreAPU());
                return apu;
            }
        }

        // 2. ESTRATEGIA 2: Buscar por palabras clave PERO con validación
        // Solo usar esto si el nombre tiene palabras clave específicas
        List<String> keywords = extractKeywords(nombreNormalizado);
        if (!keywords.isEmpty() && keywords.size() >= 2) {
            Map<Long, Integer> coincidencias = new HashMap<>();
            Map<Long, Apu> apuMap = new HashMap<>();

            for (String keyword : keywords) {
                if (keyword.length() < 3) continue;

                List<Apu> encontrados = apuDao.findByNombreAPUContainingIgnoreCase(keyword);
                for (Apu apu : encontrados) {
                    apuMap.putIfAbsent(apu.getIdAPU(), apu);
                    coincidencias.put(apu.getIdAPU(),
                            coincidencias.getOrDefault(apu.getIdAPU(), 0) + 1);
                }
            }

            if (!apuMap.isEmpty()) {
                // Solo usar si hay coincidencia de al menos 2 palabras clave
                Apu mejorApu = apuMap.values().stream()
                        .filter(a -> coincidencias.getOrDefault(a.getIdAPU(), 0) >= 2)
                        .max(Comparator.comparingInt(a -> coincidencias.getOrDefault(a.getIdAPU(), 0)))
                        .orElse(null);

                if (mejorApu != null) {
                    // ADVERTENCIA: usando coincidencia parcial
                    advertencias.add("⚠️ APU '" + nombreBuscado + "' no encontrado exactamente. " +
                            "Se usó coincidencia parcial: '" + mejorApu.getNombreAPU() + "' " +
                            "(" + coincidencias.get(mejorApu.getIdAPU()) + " palabras clave coinciden)");

                    System.out.println("⚠️ Usando coincidencia parcial para " +nombreBuscado+ " :" + mejorApu.getNombreAPU());
                    return mejorApu;
                }
            }
        }

        // 3. ESTRATEGIA 3: Coincidencia parcial (conteniendo)
        List<Apu> apusContienen = apuDao.findByNombreAPUContainingIgnoreCase(nombreBuscado);
        if (!apusContienen.isEmpty()) {
            // Si hay múltiples, elegir el que tenga más palabras coincidentes
            Apu mejorApu = apusContienen.stream()
                    .max(Comparator.comparingDouble(a -> {
                        String nombreApu = normalizeText(a.getNombreAPU());
                        Set<String> palabrasBuscadas = new HashSet<>(Arrays.asList(nombreNormalizado.split("\\s+")));
                        Set<String> palabrasApu = new HashSet<>(Arrays.asList(nombreApu.split("\\s+")));

                        long coincidenciasPalabras = palabrasBuscadas.stream()
                                .filter(p -> p.length() > 2)
                                .filter(palabrasApu::contains)
                                .count();

                        return (double) coincidenciasPalabras / palabrasBuscadas.size();
                    }))
                    .orElse(apusContienen.get(0));

            // ADVERTENCIA: usando coincidencia parcial
            advertencias.add("⚠️ APU '" + nombreBuscado + "' no encontrado exactamente. " +
                    "Se usó coincidencia parcial: '" + mejorApu.getNombreAPU() + "'");

            System.out.println("⚠️ Usando coincidencia parcial para: " + nombreBuscado + " " +  mejorApu.getNombreAPU());
            return mejorApu;
        }

        // No se encontró
        errores.add(nombreBuscado + " (normalizado: " + nombreNormalizado + ")");
        return null;
    }

    /**
     * Valida y obtiene APUs por nombre con estrategias flexibles
     */
    private Map<Long, Double> validateAndGetApusByName(Map<String, Double> actividadesPorNombre) {
        Map<Long, Double> actividades = new LinkedHashMap<>();
        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        System.out.println("Buscando " + actividadesPorNombre.size() + " APUs en el sistema");

        for (Map.Entry<String, Double> entry : actividadesPorNombre.entrySet()) {
            String nombreBuscado = entry.getKey();
            Double cantidad = entry.getValue();

            // Buscar APU con estrategias flexibles
            Apu apuEncontrado = findApuByFlexibleName(nombreBuscado, errores, advertencias);

            if (apuEncontrado != null) {
                // Verificar que no esté duplicado (mismo APU con diferentes nombres)
                if (actividades.containsKey(apuEncontrado.getIdAPU())) {
                    advertencias.add("APU duplicado: '" + nombreBuscado + "' y '" +
                            actividadesPorNombre.entrySet().stream()
                                    .filter(e -> e.getValue().equals(apuEncontrado.getIdAPU()))
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .orElse("") + "' son el mismo APU");
                }

                actividades.put(apuEncontrado.getIdAPU(), cantidad);
                System.out.println("APU encontrado: " + nombreBuscado + " -> " + apuEncontrado.getNombreAPU());

            } else {
                errores.add(nombreBuscado);
                System.err.println("❌ APU no encontrado: " + nombreBuscado);
            }
        }

        // Mostrar advertencias
        if (!advertencias.isEmpty()) {
            System.err.println("Advertencias en la importación:");
            advertencias.forEach(System.err::println);
        }

        // Lanzar error si hay APUs no encontrados
        if (!errores.isEmpty()) {
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("❌ ERROR: Los siguientes APUs no existen en el sistema:\n\n");

            for (String error : errores) {
                mensaje.append("   • ").append(error).append("\n");
            }

            mensaje.append("\n Sugerencias para resolver el problema:\n");
            mensaje.append("   1. Verifique que los nombres estén escritos correctamente\n");
            mensaje.append("   2. Puede usar solo partes del nombre (ej: 'PASAMUROS' en lugar del nombre completo)\n");
            mensaje.append("   3. El sistema busca por coincidencias parciales y palabras clave\n");
            mensaje.append("   4. Descargue la plantilla de ejemplo para ver nombres válidos\n");
            mensaje.append("\n APUs disponibles en el sistema:\n");

            // Mostrar algunos APUs similares como sugerencia
            for (String nombreErroneo : errores) {
                List<String> sugerencias = findSimilarApis(nombreErroneo);
                if (!sugerencias.isEmpty()) {
                    mensaje.append("   • Para '").append(nombreErroneo).append("' → sugiero: ");
                    mensaje.append(String.join(", ", sugerencias)).append("\n");
                }
            }

            throw new IllegalArgumentException(mensaje.toString());
        }

        return actividades;
    }

    /**
     * Encuentra APUs similares para sugerencias
     */
    private List<String> findSimilarApis(String nombre) {
        List<String> sugerencias = new ArrayList<>();
        String normalized = normalizeText(nombre);
        List<String> keywords = extractKeywords(normalized);

        if (keywords.isEmpty()) return sugerencias;

        // Buscar APUs que contengan al menos una palabra clave
        Set<Apu> apusEncontrados = new HashSet<>();
        for (String keyword : keywords) {
            apusEncontrados.addAll(apuDao.findByNombreAPUContainingIgnoreCase(keyword));
        }

        // Limitar a 5 sugerencias
        return apusEncontrados.stream()
                .limit(5)
                .map(Apu::getNombreAPU)
                .collect(Collectors.toList());
    }

    // Métodos auxiliares (extractDateFromCell, extractCoordinates, isTotalRow)
    private LocalDate extractDateFromCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue());
            }
        } catch (Exception e) {
            System.out.println("Error al extraer fecha de celda: " + e.getMessage());
        }
        return null;
    }

    private Double[] extractCoordinates(Cell cell) {
        try {
            String coordsStr = cell.getStringCellValue();
            // Formato: "N=4.75555, E=-74.05555"
            String[] parts = coordsStr.replace("N=", "").replace("E=", "").split(", ");
            return new Double[]{
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1])
            };
        } catch (Exception e) {
            System.out.println("Error al extraer coordenadas: " + e.getMessage());
            return new Double[]{0.0, 0.0}; // Valores por defecto
        }
    }

    // Importar usando IDs (no usar por ahora)
    private Map<Long, Double> extractActivities(Sheet hoja) {
        Map<Long, Double> actividades = new LinkedHashMap<>();
        int rowIndex = 5; // Empieza después de los encabezados

        while (rowIndex <= hoja.getLastRowNum()) {
            Row row = hoja.getRow(rowIndex);
            if (row == null) {
                rowIndex++;
                continue;
            }

            // Verificar si es la fila de TOTAL
            if (isTotalRow(row)) {
                break;
            }

            try {
                // Obtener ID APU (columna 0)
                Cell idCell = row.getCell(0);
                if (idCell == null || idCell.getCellType() == CellType.BLANK) {
                    rowIndex++;
                    continue;
                }

                Long idAPU = (long) idCell.getNumericCellValue();

                // Obtener cantidad (columna 3)
                Cell cantidadCell = row.getCell(3);
                if (cantidadCell == null || cantidadCell.getCellType() == CellType.BLANK) {
                    rowIndex++;
                    continue;
                }

                Double cantidad = cantidadCell.getNumericCellValue();

                if (idAPU != null && cantidad != null && cantidad > 0) {
                    actividades.put(idAPU, cantidad);
                    System.out.println("APU ID: " + idAPU +" Cantidad: " + cantidad);
                }
            } catch (Exception e) {
                System.err.println("Error al leer fila: " + rowIndex + " " + e.getMessage());
            }
            rowIndex++;
        }

        return actividades;
    }

    private boolean isTotalRow(Row row) {
        Cell cell = row.getCell(4);
        if (cell == null) return false;

        try {
            String value = cell.getStringCellValue();
            return value != null && value.contains("TOTAL OBRA:");
        } catch (Exception e) {
            return false;
        }
    }

    // Clase interna para datos extraídos
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ObraImportData {
        private String nombreObra;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Double cooNObra;
        private Double cooEObra;
        private Map<String, Double> actividadesPorNombre; // Cambio: ahora usa nombres
    }
}




