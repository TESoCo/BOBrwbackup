package com.example.servicio;

import com.example.dao.InformacionComercialDao;
import com.example.dao.MaterialDao;
import com.example.dao.ProveedorDao;
import com.example.domain.InformacionComercial;
import com.example.domain.Material;
import com.example.domain.Persona;
import com.example.domain.Proveedor;
import com.example.dto.MaterialScrapedDTO;
import com.example.scraper.EasyConScraper;
import com.example.scraper.HomecenterScraper;
import com.example.scraper.ScrapingSource;
import com.example.servicio.WebScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

        for (MaterialScrapedDTO dto : materialesDTO) {
            try {
                // Verificar si el material ya existe
                Optional<Material> existente = materialDao.findByNombreMaterial(dto.getNombre());

                if (existente.isEmpty()) {
                    // Crear nueva entidad Material
                    Material material = new Material();
                    material.setNombreMaterial(dto.getNombre());
                    material.setDescripcionMaterial(dto.getDescripcion());
                    material.setUnidadMaterial(dto.getUnidad());
                    material.setPrecioMaterial(dto.getPrecio());

                    // Buscar o crear proveedor
                    Proveedor proveedor = encontrarOCrearProveedor(dto);

                    // Agregar material al proveedor (según relación)
                    // Revisar relación Material-Proveedor
                    // Si es ManyToMany, sería proveedor.getMaterialList().add(material)
                    proveedor.getMaterialList().add(material);

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
        // Buscar proveedor por nombre (Persona)
        Proveedor proveedor = proveedorServicio.buscarPorNombre(dto.getNombre());
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
                    // Inicializar info comercial y la asignamos al proveedor
                    InformacionComercial informacionComercial;
                    if (proveedor.getInformacionComercial() == null) {
                        informacionComercial = new InformacionComercial();
                        informacionComercial.setNitRut(dto.getProveedorNit());
                        informacionComercial.setCorreoElectronico(dto.getProveedorCorreo());
                        // Valores por defecto para campos obligatorios
                        informacionComercial.setDireccion("Por definir");
                        informacionComercial.setBanco("Por definir");
                        informacionComercial.setNumCuenta("00000000");
                        informacionComercial.setFormaPago("Contado");
                        informacionComercial.setProducto("Materiales de construcción");
                        //Guardar nueva Información comercial
                        informacionComercial = infoComServicio.salvar(informacionComercial);
                        proveedor.setInformacionComercial(informacionComercial);
                    }

                    // Inicializar persona y la asignamos al proveedor
                    Persona persona;
                    if (proveedor.getIdPersona() == null) {
                        persona = new Persona();
                        persona.setNombre("Tienda en línea" + dto.getProveedorNombre());
                        persona.setApellido("obtenido con IA");
                        persona.setTelefono(dto.getProveedorTelefono());
                        persona.setCorreo(dto.getProveedorCorreo());
                        proveedor.setIdPersona(persona);
                    }

                    // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                    proveedor = proveedorServicio.guardar(proveedor);

                } else {
                    //Si encuentra una entidad comercial, solo habría que crear la persona y el proveedor
                    System.out.println("Entidad comercial localizada, NIT/RUT: " + dto.getProveedorNit()
                            + ". Se creará nuevo Proveedor " + dto.getProveedorNombre() + " relacionado a esta entidad");

                    //CREAR NUEVO PROVEEDOR y asignarle la entidad encontrada
                    proveedor = new Proveedor();
                    proveedor.setInformacionComercial(infoComProv);

                    // Inicializar persona y la asignamos al proveedor
                    Persona persona;
                    if (proveedor.getIdPersona() == null) {
                        persona = new Persona();
                        persona.setNombre("Tienda en línea" + dto.getProveedorNombre());
                        persona.setApellido("obtenido con IA");
                        persona.setTelefono(dto.getProveedorTelefono());
                        persona.setCorreo(dto.getProveedorCorreo());
                        proveedor.setIdPersona(persona);
                    }

                    // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                    proveedor = proveedorServicio.guardar(proveedor);

                }
            }else{
                //Si encuentra al proveedor por NIT, pero no por nombre; significa que la entidad comercial existe pero la persona no.
                //También puede ser que el nombre esté mal escrito, asi que si encontramos una persona al momento de la inicialización ejamos esa.
                // Inicializar persona y la asignamos al proveedor
                Persona persona;
                if (proveedor.getIdPersona() == null) {
                    persona = new Persona();
                    persona.setNombre("Tienda en línea" + dto.getProveedorNombre());
                    persona.setApellido("obtenido con IA");
                    persona.setTelefono(dto.getProveedorTelefono());
                    persona.setCorreo(dto.getProveedorCorreo());
                    proveedor.setIdPersona(persona);
                }

                // ⚠️ IMPORTANTE: Guardar el nuevo proveedor
                proveedor = proveedorServicio.guardar(proveedor);
            }
        }
        //Finalmente, si encuentra un proveedor al principio simplemente lo devuelve:
        return proveedor;
    }
}