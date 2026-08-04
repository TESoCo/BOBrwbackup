package com.example.servicio;

import com.example.dao.InformacionComercialDao;
import com.example.dao.MaterialDao;
import com.example.dao.PrecioMaterialDao;
import com.example.dao.ProveedorDao;
import com.example.domain.*;
import com.example.dto.MaterialScrapedDTO;
import com.example.scraper.EasyConScraper;
import com.example.scraper.HomecenterScraper;
import com.example.scraper.ScrapingSource;
import com.example.servicio.WebScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class WebScraperServiceImp implements WebScraperService {

    private final List<ScrapingSource> fuentes;

    @Autowired
    private MaterialDao materialDao;

    @Autowired
    private ProveedorServicio proveedorServicio;
    @Autowired
    private InformacionComercialDao informacionComercialDao;
    @Autowired
    private InfoComServicio infoComServicio;
    @Autowired
    private PersonaServicio personaServicio;
    @Autowired
    private PrecioMaterialDao precioMaterialDao;
    @Autowired
    private MaterialServicio materialServicio;

    public WebScraperServiceImp() {
        this.fuentes = new ArrayList<>();
        this.fuentes.add(new HomecenterScraper());
        this.fuentes.add(new EasyConScraper());
        // TODO agregar más fuentes aquí
    }

    @Override
    public CompletableFuture<List<MaterialScrapedDTO>> buscarMateriales(String termino) {
        return CompletableFuture.supplyAsync(() -> buscarMaterialesSincrono(termino));
    }

    @Override
    public List<MaterialScrapedDTO> buscarMaterialesSincrono(String termino) {
        List<MaterialScrapedDTO> todos = new ArrayList<>();

        System.out.println("\n=== INICIANDO BÚSQUEDA DE: " + termino + " ===\n");

        for (ScrapingSource fuente : fuentes) {
            try {
                System.out.println("🔍 Scraping desde: " + fuente.getName());
                List<MaterialScrapedDTO> resultados = fuente.scrape(termino);
                todos.addAll(resultados);
                System.out.println("✅ " + fuente.getName() + ": " + resultados.size() + " materiales encontrados\n");
            } catch (Exception e) {
                System.err.println("❌ Error en " + fuente.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("=== TOTAL MATERIALES ENCONTRADOS: " + todos.size() + " ===\n");
        return todos;
    }

    @Override
    public int importarMateriales(List<MaterialScrapedDTO> materialesDTO) {
        int importados = 0;
        System.out.println("Verificación importar materiales");
        for (MaterialScrapedDTO dto : materialesDTO) {
            try {
                System.out.println("Procesando: " + dto.getNombre());
                System.out.println("Proveedor: " + dto.getProveedorNombre());
                System.out.println("Correo: " + dto.getProveedorCorreo());
                System.out.println("NIT: " + dto.getProveedorNit());

                // Verificar si el material ya existe
                Optional<Material> existente = materialDao.findByNombreMaterial(dto.getNombre());

                if (existente.isEmpty()) {
                    // Crear nueva entidad Material
                    Material material = new Material();
                    material.setNombreMaterial(dto.getNombre());
                    material.setDescripcionMaterial(dto.getDescripcion());
                    material.setUnidadMaterial(dto.getUnidad());

                    // Buscar o crear proveedor
                    Proveedor proveedor = encontrarOCrearProveedor(dto);

                    // Agregar material al proveedor (según relación)
                    proveedor.getMaterialList().add(material);
                    material.getProveedorList().add(proveedor);

                    // Agregar precio activo (relacionado a proveedor)
                    materialServicio.asignarPrecioAProveedor(material.getIdMaterial(), proveedor.getIdProveedor(), dto.getPrecio(),dto.getPrecio());

                    // Guardar usando tu DAO
                    materialDao.save(material);
                    importados++;

                    System.out.println("✅ Importado: " + material.getNombreMaterial());
                } else {
                    System.out.println("⏭️ Ya existe: " + dto.getNombre());
                }

            } catch (Exception e) {
                System.err.println("❌ Error importando: " + dto.getNombre() + " - " + e.getMessage());
            }
        }
        return importados;
    }

    private Proveedor encontrarOCrearProveedor(MaterialScrapedDTO dto) {
        System.out.println("=== DATOS DEL DTO ===");
        System.out.println("Nombre del MATERIAL: " + dto.getNombre());
        System.out.println("Nombre del PROVEEDOR: " + dto.getProveedorNombre());
        System.out.println("NIT del proveedor: " + dto.getProveedorNit());
        System.out.println("Correo proveedor: " + dto.getProveedorCorreo());
        System.out.println("Teléfono proveedor: " + dto.getProveedorTelefono());

        // Valores por defecto si son null
        if (dto.getProveedorNit() == null) dto.setProveedorNit("NIT_NO_DISPONIBLE");
        if (dto.getProveedorCorreo() == null) dto.setProveedorCorreo("no-disponible@proveedor.com");
        if (dto.getProveedorTelefono() == null) dto.setProveedorTelefono("0000000");

        // Buscar proveedor por nombre (Persona)
        System.out.println("buscando proveedor: " + dto.getProveedorNombre());
        Proveedor proveedor = proveedorServicio.buscarPorNombre(dto.getProveedorNombre());
        //Si no encuentra un proveedor por nombre, buscar proveedor por NIT
        if (proveedor == null) {
            System.out.println("nombre proveedor no encontrado: " + dto.getProveedorNombre() +
                    " - buscando NIT: " + dto.getProveedorNit());
            proveedor = proveedorServicio.buscarPorNit(dto.getProveedorNit());
            //Si no encuentra un proveedor de esas formas, buscar información comercial por NIT
            if (proveedor == null) {
                System.out.println("NIT proveedor no encontrado, buscando información comercial relacionada...");
                InformacionComercial infoComProv = infoComServicio.localizarPorNitRut(dto.getProveedorNit());
                //Si no encuentra una entidad comercial, creamos las tres entidades (persona, infoCom, proveedor)
                if (infoComProv == null) {
                    System.out.println("Entidad comercial no encontrada por NIT/RUT: " + dto.getProveedorNit()
                            + "Se creará nuevo Proveedor " + dto.getProveedorNombre() + "y nueva entidad comercial con NIT/RUT " + dto.getProveedorNit());

                    //CREAR NUEVO PROVEEDOR
                    proveedor = new Proveedor();
                    proveedor.setNombreProveedor(dto.getProveedorNombre());
                    //Inicializar materiales relacionados al proveedor
                    proveedor.setMaterialList(new ArrayList<>());
                    // Inicializar info comercial y la asignamos al proveedor
                    InformacionComercial informacionComercial;
                    if (proveedor.getInformacionComercial() == null) {
                        informacionComercial = new InformacionComercial();
                        informacionComercial.setNitRut(dto.getProveedorNit());
                        informacionComercial.setCorreoElectronico(dto.getProveedorCorreo());
                        // Valores por defecto para campos obligatorios
                        informacionComercial.setDireccion("Calle 0 # 0-00");
                        informacionComercial.setBanco("Por definir");
                        informacionComercial.setNumCuenta("00000000");
                        informacionComercial.setFormaPago("Por definir");
                        informacionComercial.setProducto("Materiales de construcción");
                        //Guardar nueva Información comercial
                        informacionComercial = infoComServicio.salvar(informacionComercial);
                        proveedor.setInformacionComercial(informacionComercial);
                    }

                    // Inicializar persona y la asignamos al proveedor
                    Persona persona;
                    if (proveedor.getIdPersona() == null) {
                        persona = crearPersonaProveedor(dto);
                        persona = personaServicio.salvar(persona);
                        proveedor.setIdPersona(persona);
                    }
                    // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                    proveedor = proveedorServicio.guardar(proveedor);

                } else {
                    //Si encuentra una entidad comercial, solo habría que crear la persona y el proveedor
                    System.out.println("Entidad comercial localizada, NIT/RUT: " + dto.getProveedorNit()
                            + ". Se creará nuevo Proveedor " + dto.getProveedorNombre() + " relacionado a esta entidad");

                    //CREAR NUEVO PROVEEDOR y asignarle una lista de materiales y la entidad encontrada
                    proveedor = new Proveedor();

                    proveedor.setInformacionComercial(infoComProv);

                    // Inicializar persona y la asignamos al proveedor
                    Persona persona;
                    if (proveedor.getIdPersona() == null) {
                        persona = crearPersonaProveedor(dto);
                        proveedor.setIdPersona(persona);
                    }
                    // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                    proveedor = proveedorServicio.guardar(proveedor);

                }
            }else{
                /*
                * Si encuentra al proveedor por NIT, pero no por nombre; puede ser que la entidad comercial existe
                pero la persona no.
                * También puede ser que el nombre esté mal escrito, asi que si encontramos una persona relacionada
                con el proveedor con getIdPersona al momento de la inicialización, dejamos esa.
                */
                // Inicializar persona y la asignamos al proveedor
                Persona persona;
                if (proveedor.getIdPersona() == null) {
                    persona = crearPersonaProveedor(dto);
                    proveedor.setIdPersona(persona);
                }else{
                    // Actualizar solo si es necesario
                    System.out.println("Proveedor ya tiene persona asociada, actualizando datos...");
                    persona = proveedor.getIdPersona();
                    if (!persona.getNombre().equals(dto.getProveedorNombre() + " (en línea)")){
                        persona.setNombre(dto.getProveedorNombre() + " (en línea)");
                    }
                    if (dto.getFechaScraping() != null){
                        persona.setApellido("guardada el " + dto.getFechaScraping());
                    }else{
                        persona.setApellido("guardada el " + LocalDate.now());
                    }
                    persona.setTelefono(dto.getProveedorTelefono());
                    persona.setCorreo(dto.getProveedorCorreo());
                    personaServicio.salvar(persona);
                }
                // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                proveedor = proveedorServicio.guardar(proveedor);
            }
        }
        //Finalmente, si encuentra un proveedor por nombre al principio simplemente lo devuelve:
        System.out.println("✅ Proveedor encontrado por nombre: " + proveedor.getIdProveedor() +
                " - " + dto.getProveedorNombre());
        return proveedor;
    }

    private Persona crearPersonaProveedor(MaterialScrapedDTO dto){
        Persona personaProveedor = new Persona();
        personaProveedor.setNombre(dto.getProveedorNombre() + " (en línea)");
        if (dto.getFechaScraping() != null){
            personaProveedor.setApellido("guardada el " + dto.getFechaScraping());
        }else{
            personaProveedor.setApellido("guardada el " + LocalDate.now());
        }
        personaProveedor.setTelefono(dto.getProveedorTelefono());
        personaProveedor.setCorreo(dto.getProveedorCorreo());
        return personaProveedor;
    }

    @Override
    public boolean actualizarPrecioMaterialDesdeScraper(Long materialId, MaterialScrapedDTO dto) {
        try {
            System.out.println("\n=== ACTUALIZANDO PRECIO DESDE SCRAPER ===");
            System.out.println("Material ID: " + materialId);
            System.out.println("Nombre: " + dto.getNombre());
            System.out.println("Proveedor: " + dto.getProveedorNombre());
            System.out.println("Precio: " + dto.getPrecio());

            // 1. Buscar el material existente
            Optional<Material> materialOpt = materialDao.findById(materialId);
            if (materialOpt.isEmpty()) {
                System.err.println("❌ Material no encontrado con ID: " + materialId);
                return false;
            }

            Material material = materialOpt.get();



            // 2. Buscar o crear el proveedor
            Proveedor proveedor = encontrarOCrearProveedor(dto);

            // Verificar si el proveedor ya está asociado al material
            boolean proveedorAsociado = material.getProveedorList().stream()
                    .anyMatch(p -> p.getIdProveedor().equals(proveedor.getIdProveedor()));

            if (!proveedorAsociado) {
                material.getProveedorList().add(proveedor);
                proveedor.getMaterialList().add(material);
                materialDao.save(material);
                System.out.println("  Proveedor asociado al material");
            }

            // 3. Crear NUEVO precio (manteniendo historial)
            PrecioMaterial nuevoPrecio = new PrecioMaterial(material, proveedor, dto.getPrecio());
            nuevoPrecio.setFechaVigenciaDesde(LocalDateTime.now());
            nuevoPrecio.setActivo(true);

            // 4. Desactivar precios anteriores de este proveedor
            List<PrecioMaterial> preciosAnteriores = material.getPreciosPorProveedor();
            for (PrecioMaterial p : preciosAnteriores) {
                if (p.getProveedor().getIdProveedor().equals(proveedor.getIdProveedor()) &&
                        p.getActivo() && p.getFechaVigenciaHasta() == null) {
                    p.setFechaVigenciaHasta(LocalDateTime.now());
                    p.setActivo(false);
                    precioMaterialDao.save(p);
                }
            }

            // 5. Guardar nuevo precio
            precioMaterialDao.save(nuevoPrecio);

            // 6. Actualizar unidad del material si es diferente
            if (dto.getUnidad() != null && !dto.getUnidad().isEmpty() &&
                    !dto.getUnidad().equals(material.getUnidadMaterial())) {
                material.setUnidadMaterial(dto.getUnidad());
                materialDao.save(material);
            }

            // ACTUALIZAR NOMBRE si es diferente
            if (dto.getNombre() != null && !dto.getNombre().isEmpty() &&
                    !dto.getNombre().equals(material.getNombreMaterial())) {
                material.setNombreMaterial(dto.getNombre());
                System.out.println("  Nombre actualizado: " + dto.getNombre());
            }

            // ACTUALIZAR DESCRIPCIÓN si es diferente
            if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty() &&
                    (material.getDescripcionMaterial() == null ||
                            !dto.getDescripcion().equals(material.getDescripcionMaterial()))) {
                material.setDescripcionMaterial(dto.getDescripcion());
                System.out.println("  Descripción actualizada");
            }

            // Guardar cambios
            materialDao.save(material);

            System.out.println("✅ Precio actualizado correctamente");
            System.out.println("   Nuevo precio: $" + dto.getPrecio());
            System.out.println("   Proveedor: " + proveedor.getNombreProveedor());

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error actualizando precio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}