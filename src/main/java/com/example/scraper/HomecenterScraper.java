package com.example.scraper;

import com.example.dto.MaterialScrapedDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HomecenterScraper implements ScrapingSource {

    @Override
    public String getName() {
        return "Homecenter";
    }

    @Override
    public String getNit() {
        return "800.242.106-2";  // NIT real de Homecenter Colombia
    }

    @Override
    public String getCorreo() {
        return "servicioalcliente@homecenter.com.co";
    }

    @Override
    public String getTelefono() {
        return "6017444444";  // Teléfono de Homecenter
    }

    @Override
    public List<MaterialScrapedDTO> scrape(String termino) {
        List<MaterialScrapedDTO> resultados = new ArrayList<>();

        try {
            // Construir URL de búsqueda - USANDO LA ESTRUCTURA CORRECTA
            String url = "https://www.homecenter.com.co/homecenter-co/search/?Ntt="
                    + termino.replace(" ", "+");

            System.out.println("🌐 Conectando a: " + url);

            // Configurar la conexión como un navegador real
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "es-CO,es;q=0.8,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .timeout(15000)
                    .get();

            // SELECTOR CORRECTO: Los productos están en div con clase "product-wrapper"
            Elements productos = doc.select(".product-wrapper");

            System.out.println("📦 Productos encontrados: " + productos.size());

            for (Element producto : productos) {
                try {
                    MaterialScrapedDTO material = new MaterialScrapedDTO();

                    // === NOMBRE DEL PRODUCTO ===
                    // Está dentro de h2 con clase "product-title"
                    Element nombreElem = producto.selectFirst(".product-title");
                    if (nombreElem == null) {
                        continue; // Saltar si no hay nombre
                    }
                    String nombre = nombreElem.text().trim();
                    material.setNombre(nombre);

                    // === PRECIO ===
                    // El precio está en span con clase "parsedPrice"
                    Element precioElem = producto.selectFirst(".parsedPrice");
                    if (precioElem != null) {
                        String precioText = precioElem.text()
                                .replace("$", "")
                                .replace(".", "")
                                .replace("COP", "")
                                .trim();
                        try {
                            BigDecimal precio = new BigDecimal(precioText);
                            material.setPrecio(precio);
                        } catch (NumberFormatException e) {
                            material.setPrecio(BigDecimal.ZERO);
                        }
                    } else {
                        material.setPrecio(BigDecimal.ZERO);
                    }

                    // === MARCA ===
                    // La marca está en div con clase "product-brand"
                    Element marcaElem = producto.selectFirst(".product-brand");
                    if (marcaElem != null) {
                        material.setMarca(marcaElem.text().trim());
                    }

                    // === UNIDAD ===
                    // La unidad está en span con clase "price-unit"
                    Element unidadElem = producto.selectFirst(".price-unit");
                    if (unidadElem != null) {
                        material.setUnidad(unidadElem.text().trim());
                    } else {
                        material.setUnidad(inferirUnidad(nombre));
                    }

                    // === CATEGORÍA ===
                    material.setCategoria(clasificarCategoria(nombre));

                    // === DESCRIPCIÓN ===
                    material.setDescripcion(generarDescripcion(nombre, material.getMarca()));

                    // === DATOS DEL PROVEEDOR ===
                    material.setProveedorNombre(getName());
                    material.setProveedorNit(getNit());
                    material.setProveedorCorreo(getCorreo());
                    material.setProveedorTelefono(getTelefono());

                    System.out.println("  verificación homecenterscraper ");
                    System.out.println("  Nombre: " + material.getNombre());
                    System.out.println("  ProveedorNombre: " + material.getProveedorNombre());
                    System.out.println("  NIT: " + material.getProveedorNit());
                    System.out.println("  Correo: " + material.getProveedorCorreo());
                    System.out.println("  Teléfono: " + material.getProveedorTelefono());

                    resultados.add(material);
                    System.out.println("✅ " + material.getNombre() + " - $" + material.getPrecio() + " - " + material.getUnidad());

                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando producto: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            return new ArrayList<>();
        }

        return resultados;
    }

    private String inferirUnidad(String nombre) {
        nombre = nombre.toLowerCase();
        if (nombre.contains("saco") || nombre.contains("bulto")) return "SACO";
        if (nombre.contains("metro cuadrado") || nombre.contains("m2")) return "M2";
        if (nombre.contains("metro cubico") || nombre.contains("m3")) return "M3";
        if (nombre.contains("metro") || nombre.contains("m.")) return "M";
        if (nombre.contains("kilo") || nombre.contains("kg")) return "KG";
        if (nombre.contains("litro") || nombre.contains("l")) return "L";
        if (nombre.contains("unidad") || nombre.contains("pieza")) return "UND";
        if (nombre.contains("caja")) return "CAJA";
        if (nombre.contains("paquete")) return "PAQ";
        return "UND";
    }

    private String clasificarCategoria(String nombre) {
        nombre = nombre.toLowerCase();
        if (nombre.contains("cemento")) return "CEMENTOS";
        if (nombre.contains("varilla") || nombre.contains("fierro") || nombre.contains("acero")) return "ACEROS";
        if (nombre.contains("madera") || nombre.contains("pino")) return "MADERAS";
        if (nombre.contains("ladrillo") || nombre.contains("bloque")) return "LADRILLOS";
        if (nombre.contains("pintura")) return "PINTURAS";
        if (nombre.contains("tuberia") || nombre.contains("pvc")) return "TUBERIA";
        if (nombre.contains("herramienta") || nombre.contains("taladro")) return "HERRAMIENTAS";
        if (nombre.contains("molde") || nombre.contains("cable")) return "ELECTRICOS";
        return "OTROS";
    }

    private String generarDescripcion(String nombre, String marca) {
        StringBuilder desc = new StringBuilder();
        desc.append(nombre);
        if (marca != null && !marca.isEmpty() && !marca.equals("Multimarcas")) {
            desc.append(" - Marca ").append(marca);
        }
        desc.append(". Disponible en Homecenter Colombia.");
        return desc.toString();
    }
}