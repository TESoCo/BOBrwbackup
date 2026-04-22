package com.example.scraper;

import com.example.dto.MaterialScrapedDTO;
import java.util.List;

public interface ScrapingSource {
    String getName();           // Nombre de la fuente
    String getNit();            // NIT del proveedor
    String getCorreo();       // Email de contacto
    List<MaterialScrapedDTO> scrape(String termino);  // Retorna DTOs, no entidades
}
