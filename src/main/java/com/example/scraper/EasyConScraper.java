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
import java.util.Objects;
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
            String terminoBusqueda = termino.toLowerCase();//lo que el usuario está buscando
            // URL de búsqueda de Easy Colombia
            // El endpoint oficial de búsqueda de VTEX - Easy usa VTEX que tiene un endpoint con la información completa sin tener que pescar del html
            String url = "https://www.easy.com.co/api/catalog_system/pub/products/search/"
                    + terminoBusqueda.replace(" ", "+%20");
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

            ObjectMapper mapper = new ObjectMapper();
            JsonNode productosJson = mapper.readTree(jsonResponse); // El root ya es el array
            if (productosJson != null && productosJson.isArray()) {
                for (JsonNode p : productosJson) {
                    try {
                        MaterialScrapedDTO material = new MaterialScrapedDTO();

                        // 0. Prueba de relevancia VTEX es un fastidio
                        String nombre = p.path("productName").asText().toLowerCase();//los nombres de los productos encontrados

                        // Dividimos el término en palabras para verificar
                        String[] palabrasClave = terminoBusqueda.split(" ");
                        boolean esRelevante = false;

                        //TODO: ¿Hay palabras clave válidas de menos de tres letras?... parece que no
                        for (String palabra : palabrasClave) {
                            if (palabra.length() > 3 && nombre.contains(palabra)) {
                                esRelevante = true;
                                break;
                            }
                        }

                        if (!esRelevante) {
                            continue; //lo que no tenga nada que ver, se los salta
                        }

                        // 1. Nombre y Marca (Nivel raíz del producto)
                        material.setNombre(p.path("productName").asText());
                        material.setMarca(p.path("brand").asText());

                        // 2. Navegación profunda para el precio
                        // .path() es más seguro que .get() porque evita NullPointerException si el nodo no existe
                        //root->items->(first)->sellers->(first)->comertialoffer->price
                        JsonNode firstItem = p.path("items").get(0);
                        JsonNode firstSeller = firstItem.path("sellers").get(0);
                        JsonNode offer = firstSeller.path("commertialOffer");

                        // Obtenemos el precio de venta (Price)
                        double precioValor = offer.path("Price").asDouble();

                        // Si el precio es 0, intentamos con ListPrice (precio de lista)
                        if (precioValor == 0) {
                            precioValor = offer.path("ListPrice").asDouble();
                        }

                        material.setPrecio(BigDecimal.valueOf(precioValor));

                        // 3. Stock (Dato extra valioso para gestión de inventarios)
                        int stock = offer.path("AvailableQuantity").asInt();

                        // 4. URL del producto
                        material.setUrlFuente(p.path("link").asText());

                        // 5. Limpieza de unidad
                        // en easy está guardado en una campo del JSON
                        //root->items->(first)->measurementUnit
                        JsonNode firstUnits = firstItem.path("measurementUnit");
                        String unidadesProducto = firstUnits.asText("noHayUnJSON");

                        if (Objects.equals(unidadesProducto, "noHayUnJSON")) {
                            //Si no encuentra la información en el JSON, tratar de inferirla del nombre
                            material.setUnidad(extractUnidadFromName(material.getNombre()));
                        }else{
                            material.setUnidad(unidadesProducto);
                        }

                        // === 6. DESCRIPCIÓN ===
                        String descProducto = p.path("description").asText("noHayUnJSON");

                        if (Objects.equals(descProducto, "noHayUnJSON")) {
                            //Si no encuentra la información en el JSON, tratar de inferirla del nombre
                            material.setDescripcion(generarDescripcion(material.getNombre(), material.getMarca()));
                        }else{
                            material.setUnidad(descProducto);
                        }


                        // === 7. DATOS DEL PROVEEDOR ===
                        material.setProveedorNombre(getName());
                        material.setProveedorNit(getNit());
                        material.setProveedorCorreo(getCorreo());

                        resultados.add(material);
                        System.out.println("✅ Easy: " + material.getNombre() + " - $" + material.getPrecio() + " - " + material.getUnidad());

                    } catch (Exception e) {
                        System.err.println("⚠️ Error procesando nodo JSON de Easy: " + e.getMessage());
                    }
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
        //TODO: mejorar extractUnidadFromName
        if (nombre == null) return "Unidad";
        String nombreUpper = nombre.toUpperCase();
        if (nombreUpper.contains("GL") || nombreUpper.contains("GAL") || nombreUpper.contains("GALS")) return "Galones";
        if (nombreUpper.contains("2.5GL") || nombreUpper.contains("2.5 GL") || nombreUpper.contains("X2.5GAL")) return "2.5 Galones";
        if (nombreUpper.contains("1 GL") || nombreUpper.contains("X1GAL") || nombreUpper.contains("1GL")) return "1 Galón";
        if (nombreUpper.contains("1/4GL") || nombreUpper.contains("1/4 GL") || nombreUpper.contains("X1/4GAL") || nombreUpper.contains("0.25 GAL")) return "1/4 Galón";
        if (nombreUpper.contains("LT") || nombreUpper.contains("LITRO")) return "Litro";
        if (nombreUpper.contains("LTS") || nombreUpper.contains("LITROS")) return "Litros";
        if (nombreUpper.contains("ML") || nombreUpper.contains("MILILITRO")) return "Mililitro";
        if (nombreUpper.contains("KG") || nombreUpper.contains("KILO")) return "Kilogramo";
        if (nombreUpper.contains("GR") || nombreUpper.contains("GRAMO")) return "Gramo";
        if (nombreUpper.contains(" UN") || nombreUpper.contains("UND") || nombreUpper.contains("UNIDAD")) return "Unidad";
        if (nombreUpper.contains("MT") || nombreUpper.contains("METRO")) return "Metro";
        if (nombreUpper.contains("CM") || nombreUpper.contains("CENTIMETRO")) return "Centímetro";
        if (nombreUpper.contains("MM") || nombreUpper.contains("MILIMETRO")) return "Milímetro";
        if (nombreUpper.contains("CJ") || nombreUpper.contains("CAJA")) return "Caja";
        if (nombreUpper.contains("PK") || nombreUpper.contains("PQT") || nombreUpper.contains("PAQUETE")) return "Paquete";
        if (nombreUpper.contains("BL") || nombreUpper.contains("BAG") || nombreUpper.contains("BOLSA")) return "Bolsa";
        if (nombreUpper.contains("RL") || nombreUpper.contains("ROLLO")) return "Rollo";
        if (nombreUpper.contains("PLG") || nombreUpper.contains("PLIEGO")) return "Pliego";
        if (nombreUpper.contains(" IN") || nombreUpper.contains("PULG") || nombreUpper.contains("PULGADA")) return "Pulgada";
        if (nombreUpper.contains(" FT") || nombreUpper.contains("PIE")) return "Pie";
        if (nombreUpper.contains(" LB") || nombreUpper.contains("LIBRA,")) return "Libra";
        if (nombreUpper.contains(" TN") || nombreUpper.contains("TON") || nombreUpper.contains("TONELADA")) return "Bolsa";
        return "Unidad";
    }


    private String clasificarCategoria(String nombre) {
        if (nombre == null) return "Pinturas y Accesorios";
        String nombreUpper = nombre.toUpperCase();
        String n = nombre.toLowerCase();
        if (n.contains("impermeabilizante") || n.contains("koraza")) return "Pinturas Impermeabilizantes";
        if (n.contains("baños") && n.contains("cocinas") || n.contains("acriltex")) return "Pinturas Baños y Cocinas";
        if (n.contains("ultralavable") || n.contains("viniltex")) return "Pinturas Vinílicas Interior";
        if (n.contains("t3") || n.contains("techo") || n.contains("tito pabon")) return "Pinturas para Techos";
        if (n.contains("t1") || n.contains("advanced") || n.contains("mandarina")) return "Pinturas para Fachadas";
        if (nombreUpper.contains(" UN") || nombreUpper.contains("UND") || nombreUpper.contains("UNIDAD")) return "Unidad";
        if (nombreUpper.contains("MT") || nombreUpper.contains("METRO")) return "Metro";
        return "Otros";
    }

    private String generarDescripcion(String nombre, String marca) {
        String unidad = extractUnidadFromName(nombre);
        return String.format(" %s en presentación de %s.",
                marca, unidad.toLowerCase());
    }
}