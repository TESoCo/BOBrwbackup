package com.example.scraper;

import com.example.dto.MaterialScrapedDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasyConScraper implements ScrapingSource {

    @Override
    public String getName() {
        return "Easy Colombia";
    }

    @Override
    public String getNit() {
        return "900.456.789-1";  // NIT de Easy Colombia (Cencosud)
    }

    @Override
    public String getCorreo() {
        return "servicioalcliente@easy.com.co";
    }

    @Override
    public List<MaterialScrapedDTO> scrape(String termino) {
        List<MaterialScrapedDTO> resultados = new ArrayList<>();

        try {
            // URL de búsqueda de Easy Colombia
            // El endpoint oficial de búsqueda de VTEX - Easy usa VTEX que tiene un endpoint con la información completa sin tener que pescar del html
            String url = "https://www.easy.com.co/api/catalog_system/pub/products/search/" + termino.replace(" ", "%20");
            System.out.println("🌐 Conectando a Easy Colombia: " + url);

            // Configurar la conexión como un navegador real
            String jsonResponse = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0...") // Mantén tu User-Agent
                    .header("Accept", "application/json") // Avisamos que queremos JSON
                    .header("Content-Type", "application/json")
                    .ignoreContentType(true) // ¡CRUCIAL! Esto permite que Jsoup reciba el JSON
                    .execute()
                    .body();

            // SELECTORES PARA EASY COLOMBIA
            // Los productos en Easy (VTEX IO) están dentro de 'section' con la clase 'vtex-product-summary-2-x-container'
            ObjectMapper mapper = new ObjectMapper();
            JsonNode productosJson = mapper.readTree(jsonResponse);

            for (JsonNode p : productosJson) {
                try {
                    MaterialScrapedDTO material = new MaterialScrapedDTO();

                    // 1. Nombre
                    material.setNombre(p.get("productName").asText());

                    // 2. Precio (En VTEX el precio viene en 'items' -> 'sellers' -> 'commertialOffer')
                    JsonNode offer = p.get("items").get(0).get("sellers").get(0).get("commertialOffer");
                    double precioValor = offer.get("Price").asDouble();
                    material.setPrecio(BigDecimal.valueOf(precioValor));

                    // 3. Marca
                    material.setMarca(p.get("brand").asText());

                    // 4. URL del producto
                    material.setUrlFuente(p.get("link").asText());

                    // === 5. CATEGORÍA ===
                    material.setCategoria(clasificarCategoria(material.getNombre()));

                    // === 6. DESCRIPCIÓN ===
                    material.setDescripcion(generarDescripcion(material.getNombre(), material.getMarca()));

                    // === 7. DATOS DEL PROVEEDOR ===
                    material.setProveedorNombre(getName());
                    material.setProveedorNit(getNit());
                    material.setProveedorCorreo(getCorreo());

                    resultados.add(material);
                    System.out.println("✅ Easy: " + material.getNombre() + " - $" + material.getPrecio() + " - " + material.getUnidad());

                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando producto de Easy: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error de conexión con Easy: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error general en Easy: " + e.getMessage());
            return new ArrayList<>();
        }
        return resultados;
    }

    // Métodos auxiliares (Lógica de negocio específica para Easy)

    private String extractUnidadFromName(String nombre) {
        if (nombre == null) return "Unidad";
        String nombreUpper = nombre.toUpperCase();
        if (nombreUpper.contains("5 GL") || nombreUpper.contains("5GL") || nombreUpper.contains("X5GAL") || nombreUpper.contains("5 GAL")) return "5 Galones";
        if (nombreUpper.contains("2.5GL") || nombreUpper.contains("2.5 GL") || nombreUpper.contains("X2.5GAL")) return "2.5 Galones";
        if (nombreUpper.contains("1 GL") || nombreUpper.contains("X1GAL") || nombreUpper.contains("1GL")) return "1 Galón";
        if (nombreUpper.contains("1/4GL") || nombreUpper.contains("1/4 GL") || nombreUpper.contains("X1/4GAL") || nombreUpper.contains("0.25 GAL")) return "1/4 Galón";
        if (nombreUpper.contains("LT") || nombreUpper.contains("LITRO")) return "Litro";
        return "Galón";
    }

    private String clasificarCategoria(String nombre) {
        if (nombre == null) return "Pinturas y Accesorios";
        String n = nombre.toLowerCase();
        if (n.contains("impermeabilizante") || n.contains("koraza")) return "Pinturas Impermeabilizantes";
        if (n.contains("baños") && n.contains("cocinas") || n.contains("acriltex")) return "Pinturas Baños y Cocinas";
        if (n.contains("ultralavable") || n.contains("viniltex")) return "Pinturas Vinílicas Interior";
        if (n.contains("t3") || n.contains("techo") || n.contains("tito pabon")) return "Pinturas para Techos";
        if (n.contains("t1") || n.contains("advanced") || n.contains("mandarina")) return "Pinturas para Fachadas";
        return "Pinturas Látex";
    }

    private String generarDescripcion(String nombre, String marca) {
        String unidad = extractUnidadFromName(nombre);
        return String.format("Pintura %s de alta calidad en presentación de %s. Ideal para proyectos de construcción y remodelación.",
                marca, unidad.toLowerCase());
    }
}