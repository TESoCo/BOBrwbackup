package com.example.servicio;

import com.example.dao.MaterialDao;
import com.example.dao.ProveedorDao;
import com.example.domain.Material;
import com.example.domain.Proveedor;
import com.example.dto.MaterialScrapedDTO;
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
    private ProveedorDao proveedorDao;

    public WebScraperServiceImp() {
        this.fuentes = new ArrayList<>();
        this.fuentes.add(new HomecenterScraper());
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
                    // TODO: Revisar relación Material-Proveedor
                    // Si es ManyToMany, sería proveedor.getMaterialList().add(material)

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
        // Buscar proveedor por NIT usando tu DAO
        Proveedor proveedor = proveedorDao.findByInformacionComercial_NitRut(dto.getProveedorNit());

        if (proveedor == null) {
            // TODO: Aquí crear un nuevo proveedor
            // Esto es simplificado, necesitarías crear la persona y la info comercial
            System.out.println("⚠️ Proveedor no encontrado: " + dto.getProveedorNombre() +
                    " - NIT: " + dto.getProveedorNit());
            return null;
        }

        return proveedor;
    }
}