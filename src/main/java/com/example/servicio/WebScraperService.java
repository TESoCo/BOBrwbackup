package com.example.servicio;

import com.example.dto.MaterialScrapedDTO;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WebScraperService {
    CompletableFuture<List<MaterialScrapedDTO>> buscarMateriales(String termino);
    List<MaterialScrapedDTO> buscarMaterialesSincrono(String termino);

    // Metodo que usa tus DAOs existentes para guardar
    int importarMateriales(List<MaterialScrapedDTO> materiales);
}