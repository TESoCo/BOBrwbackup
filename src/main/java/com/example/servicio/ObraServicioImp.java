package com.example.servicio;

import com.example.dao.*;
import com.example.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private APUServicio apuServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private EquipoServicio equipoServicio;


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
    public void actualizarActividadesDeObra(Long idObra, List<Long> actividadIds, List<Double> cantidades) {
        Obra obra = localizarObra(idObra);
        if (obra == null) {
            throw new RuntimeException("Obra no encontrada");
        }

        // Guardar la fecha manual actual antes de modificar
        LocalDate fechaManualActual = obra.getFechaFinManual();

        // 1. Eliminar APUs existentes
        List<ApusObra> existentes = apusObraDao.findByObra_IdObra(idObra);
        for (ApusObra ao : existentes) {
            apusObraDao.delete(ao);
        }

        // 2. Agregar nuevos APUs
        for (int i = 0; i < actividadIds.size(); i++) {
            Apu apu = apuServicio.obtenerPorId(actividadIds.get(i));
            if (apu != null && cantidades.get(i) > 0) {
                agregarApuAObraConCantidad(obra, apu, cantidades.get(i));
            }
        }

        // 3. Restaurar la fecha manual
        obra.setFechaFinManual(fechaManualActual);

        // 4. Recalcular la fecha calculada
        calcularDuracionLinealObra(obra);
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
                System.out.println("APU: " + apusObra.getApu().getNombreAPU() + ", Duración: " + apusObra.getApu().getDuracionAPU());
                duracionTotalObra = duracionTotalObra.add(apusObra.getApu().getDuracionAPU());
            };
            System.out.println("Duración total: " + duracionTotalObra);
            if(obra.getFechaIni()!=null){
                obra.setFechaFinCalculada(obra.getFechaIni().plusDays(duracionTotalObra.longValue()));
            };
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
    public Obra getObraPresupuesto(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.PRESUPUESTO.equals(o.getEtapa())).findFirst().orElse(null);
    }

    @Override
    public Obra getObraEjecucion(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.EJECUCION.equals(o.getEtapa()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Obra getObraCierre(Long idProyecto) {
        Proyecto proyecto = proyectoDao.findById(idProyecto).get();
        if (proyecto == null) return null;
        return proyecto.getObras().stream()
                .filter(o -> Obra.EtapaObra.CIERRE.equals(o.getEtapa()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ApusObra> getApusPresupuesto(Long idProyecto) {
        Obra presupuesto = getObraPresupuesto(idProyecto);
        return presupuesto != null ? presupuesto.getApusObraList() : new ArrayList<>();
    }

    @Override
    public List<ApusObra> getApusEjecucion(Long idProyecto) {
        Obra ejecucion = getObraEjecucion(idProyecto);
        return ejecucion != null ? ejecucion.getApusObraList() : new ArrayList<>();
    }

    @Override
    public List<ApusObra> getApusCierre(Long idProyecto) {
        Obra cierre = getObraCierre(idProyecto);
        return cierre != null ? cierre.getApusObraList() : new ArrayList<>();
    }

    @Override
    public boolean isPresupuestoCompletado(Long idProyecto) {
        Obra presupuesto = getObraPresupuesto(idProyecto);
        return presupuesto != null &&
                presupuesto.getApusObraList() != null &&
                !presupuesto.getApusObraList().isEmpty();
    }

    @Override
    public boolean isEjecucionActiva(Long idProyecto) {
        Obra ejecucion = getObraEjecucion(idProyecto);
        return ejecucion != null &&
                ejecucion.getApusObraList() != null &&
                !ejecucion.getApusObraList().isEmpty();
    }

    @Override
    public Proyecto encontrarPorIdentificadorObra(String identificadorUnico) {
        List<Obra> obras = obraDao.findByIdentificadorUnico(identificadorUnico);
        if (obras != null && !obras.isEmpty()) {
            return obras.get(0).getProyecto();
        }
        return null;
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
            return getObraCierre(proyecto.getIdProyecto()) == null;
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


}




