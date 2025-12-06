package com.example.controller.web;

import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.servicio.AvanceServicio;
import com.example.servicio.FotoDatoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.Base64;

@Controller
@RequestMapping("/fotodatos")
public class ControladorFotoDato {

    @Autowired
    private FotoDatoServicio fotoDatoServicio;

    @Autowired
    private AvanceServicio avanceServicio;

    // Mostrar formulario para capturar foto
    @GetMapping("/capturar/{idAvance}")
    public String mostrarCamara(@PathVariable Long idAvance, Model model) {
        Avance avance = avanceServicio.localizarAvance(idAvance);
        model.addAttribute("avance", avance);
        model.addAttribute("fotoDato", new FotoDato());
        return "fotodatos/capturarFoto";
    }

    // Guardar foto desde la cámara (usando GriFS en Mongo alta)
    @PostMapping("/guardarDesdeCamara/{idAvance}")
    public String guardarDesdeCamara(
            @PathVariable Long idAvance,
            @RequestParam("fotoFile") MultipartFile fotoFile,
            @RequestParam(value = "cooN", required = false) Double cooN,
            @RequestParam(value = "cooE", required = false) Double cooE,
            Model model) {

        try {
            if (fotoFile.isEmpty()) {
                model.addAttribute("error", "No se ha capturado ninguna foto");
                return "redirect:/avances/detalle/" + idAvance;
            }

// Verificar tamaño (máximo 8MB para MongoDB gratis)
            if (fotoFile.getSize() > 8 * 1024 * 1024) {
                model.addAttribute("error", "La imagen es demasiado grande. Máximo 8MB.");
                return "redirect:/avances/detalle/" + idAvance;
            }

            Avance avance = avanceServicio.localizarAvance(idAvance);
            // Crear entidad FotoDato
            FotoDato fotoDato = new FotoDato();
            fotoDato.setIdAvance(avance);

            fotoDato.setCooNFoto(cooN);
            fotoDato.setCooEFoto(cooE);
            fotoDato.setFechaFoto(LocalDate.now());

            fotoDatoServicio.salvar(fotoDato,fotoFile);

            return "redirect:/avances/detalle/" + idAvance;

        } catch (IOException e) {
            model.addAttribute("error", "Error al guardar la foto: " + e.getMessage());
            return "redirect:/avances/detalle/" + idAvance;
        }
    }

    // Subir foto desde archivo
    @PostMapping("/subirArchivo/{idAvance}")
    public String subirArchivo(
            @PathVariable Long idAvance,
            @RequestParam("archivoFoto") MultipartFile archivoFoto,
            @RequestParam(value = "cooN", required = false) Double cooN,
            @RequestParam(value = "cooE", required = false) Double cooE,
            Model model) {

        try {
            if (!archivoFoto.isEmpty()) {
                Avance avance = avanceServicio.localizarAvance(idAvance);

                FotoDato fotoDato = new FotoDato();
                fotoDato.setIdAvance(avance);
                fotoDato.setCooNFoto(cooN);
                fotoDato.setCooEFoto(cooE);
                fotoDato.setFechaFoto(LocalDate.now());

                fotoDatoServicio.salvar(fotoDato,archivoFoto);
            }

            return "redirect:/avances/detalle/" + idAvance;

        } catch (IOException e) {
            model.addAttribute("error", "Error al subir el archivo: " + e.getMessage());
            return "redirect:/avances/detalle/" + idAvance;
        } catch (Exception e) {
            model.addAttribute("error", "Error inesperado: " + e.getMessage());
            return "redirect:/avances/detalle/" + idAvance;
        }
    }

    // Ver detalle de foto
    @GetMapping("/ver/{idFotoDato}")
    public String verFoto(@PathVariable Long idFotoDato, Model model) {
        FotoDato fotoDato = fotoDatoServicio.localizarFotoDato(idFotoDato);
        model.addAttribute("fotoDato", fotoDato);
        return "fotodatos/verFoto";
    }

    // Eliminar foto
    @GetMapping("/eliminar/{idFotoDato}")
    public String eliminarFoto(@PathVariable Long idFotoDato) {
        try {
            FotoDato fotoDato = fotoDatoServicio.localizarFotoDato(idFotoDato);
            Long idAvance = fotoDato.getIdAvance().getIdAvance();
            fotoDatoServicio.borrar(fotoDato);// El servicio ahora maneja la eliminación tanto de MySQL como de MongoDB GridFS
            return "redirect:/avances/detalle/" + idAvance;
        } catch (Exception e) {
            return "redirect:/avances/inicioAvances";
        }
    }

    // Servir imágenes desde MongoDB GridFS
    @GetMapping("/imagen/{idFotoDato}")
    @ResponseBody
    public ResponseEntity<byte[]> mostrarImagen(@PathVariable Long idFotoDato) {
        try {
            FotoDato fotoDato = fotoDatoServicio.localizarFotoDato(idFotoDato);

            if (fotoDato == null || fotoDato.getGridfsFileId() == null) {
                return ResponseEntity.notFound().build();
            }

            // Obtener archivo desde MongoDB GridFS usando el servicio
            byte[] imageData = fotoDatoServicio.obtenerArchivoFoto(fotoDato.getGridfsFileId());

            if (imageData == null) {
                return ResponseEntity.notFound().build();
            }

            // Determinar content type
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (fotoDato.getTipoMime() != null) {
                try {
                    mediaType = MediaType.parseMediaType(fotoDato.getTipoMime());
                } catch (Exception e) {
                    // Usar JPEG por defecto si hay error
                    mediaType = MediaType.IMAGE_JPEG;
                }
            }

            // Configurar headers para correcta visualización
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType); // Asumiendo que son JPEG
            headers.setContentLength(imageData.length);
            headers.setCacheControl("max-age=3600"); // Cache por 1 hora
            headers.set("Content-Disposition", "inline; filename=\"" + fotoDato.getNombreArchivo() + "\"");

            return new ResponseEntity<>(imageData, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ NUEVOS MÉTODOS PARA PDF ============

    // 1. PDF de una sola foto
    @GetMapping("/pdf/foto/{idFotoDato}")
    @ResponseBody
    public ResponseEntity<byte[]> generarPDFFoto(@PathVariable Long idFotoDato) {
        try {
            // Obtener FotoDato con toda su información
            FotoDato fotoDato = fotoDatoServicio.localizarFotoDato(idFotoDato);
            if (fotoDato == null) {
                return ResponseEntity.notFound().build();
            }

            // Preparar datos para enviar a FastAPI
            Map<String, Object> datosPDF = prepararDatosFotoParaPDF(fotoDato);

            // Enviar a FastAPI y obtener PDF
            byte[] pdfBytes = enviarAFastAPI("/pdf/foto-completa", datosPDF);

            // Devolver PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename",
                    "fotodato_" + idFotoDato + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando PDF: " + e.getMessage()).getBytes());
        }
    }

    // 2. PDF de todas las fotos de una obra
    @GetMapping("/pdf/obra/{idObra}")
    @ResponseBody
    public ResponseEntity<byte[]> generarPDFObra(@PathVariable Long idObra) {
        try {
            // Obtener todas las fotos de la obra
            List<FotoDato> fotos = fotoDatoServicio.buscarPorIdObra(idObra);

            if (fotos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encontraron fotos para esta obra".getBytes());
            }

            // Preparar datos para enviar a FastAPI
            Map<String, Object> datosPDF = prepararDatosObraParaPDF(idObra, fotos);

            // Enviar a FastAPI y obtener PDF
            byte[] pdfBytes = enviarAFastAPI("/pdf/obra-completa", datosPDF);

            // Devolver PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            // Nombre del archivo con el nombre de la obra (si está disponible)
            String nombreObra = "obra_" + idObra;
            if (!fotos.isEmpty() && fotos.get(0).getIdAvance() != null
                    && fotos.get(0).getIdAvance().getIdObra() != null) {
                nombreObra = fotos.get(0).getIdAvance().getIdObra().getNombreObra()
                        .replace(" ", "_");
            }

            headers.setContentDispositionFormData("filename",
                    "reporte_" + nombreObra + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando PDF: " + e.getMessage()).getBytes());
        }
    }

// ============ MÉTODOS AUXILIARES PRIVADOS ============

    private Map<String, Object> prepararDatosFotoParaPDF(FotoDato fotoDato) throws IOException {
        Map<String, Object> datos = new HashMap<>();

        // Información de FotoDato
        datos.put("idFotoDato", fotoDato.getIdFotoDato());
        datos.put("fechaFoto", fotoDato.getFechaFoto() != null ?
                fotoDato.getFechaFoto().toString() : null);

        // Coordenadas
        if (fotoDato.getCooNFoto() != null && fotoDato.getCooEFoto() != null) {
            datos.put("coordenadas",
                    String.format("%.6f, %.6f", fotoDato.getCooNFoto(), fotoDato.getCooEFoto()));
            datos.put("cooN", fotoDato.getCooNFoto());
            datos.put("cooE", fotoDato.getCooEFoto());
        }

        datos.put("nombreArchivo", fotoDato.getNombreArchivo());
        datos.put("tamanioArchivo", fotoDato.getTamanioArchivo());
        datos.put("tipoMime", fotoDato.getTipoMime());

        // Información del Avance y relaciones
        if (fotoDato.getIdAvance() != null) {
            Avance avance = fotoDato.getIdAvance();
            datos.put("idAvance", avance.getIdAvance());
            datos.put("fechaAvance", avance.getFechaAvance() != null ?
                    avance.getFechaAvance().toString() : null);
            datos.put("cantidadEjecutada", avance.getCantEjec());

            // Información de la Obra
            if (avance.getIdObra() != null) {
                datos.put("nombreObra", avance.getIdObra().getNombreObra());
                datos.put("cooEObra", avance.getIdObra().getCooEObra());
                datos.put("cooNObra", avance.getIdObra().getCooNObra());
            }

            // Información del Usuario
            if (avance.getIdUsuario() != null) {
                datos.put("nombreUsuario", avance.getIdUsuario().getNombreUsuario());
                datos.put("cargoUsuario", avance.getIdUsuario().getCargo());
            }

            // Información del APU
            if (avance.getIdApu() != null) {
                datos.put("descripcionApu", avance.getIdApu().getNombreAPU());
                datos.put("unidadApu", avance.getIdApu().getUnidadesAPU());
            }

            // Información del Contratista (opcional)
            if (avance.getIdContratista() != null) {
                datos.put("nombreContratista", avance.getIdContratista().getNombreContratista());
            }
        }

        // Obtener imagen de GridFS y convertir a base64
        if (fotoDato.getGridfsFileId() != null) {
            byte[] imagenBytes = fotoDatoServicio.obtenerArchivoFoto(fotoDato.getGridfsFileId());
            if (imagenBytes != null) {
                String imagenBase64 = Base64.getEncoder().encodeToString(imagenBytes);
                datos.put("imagenBase64", imagenBase64);
            }
        }

        return datos;
    }

    private Map<String, Object> prepararDatosObraParaPDF(Long idObra, List<FotoDato> fotos) throws IOException {
        Map<String, Object> datosObra = new HashMap<>();

        // Información de la obra (del primer avance)
        if (!fotos.isEmpty() && fotos.get(0).getIdAvance() != null
                && fotos.get(0).getIdAvance().getIdObra() != null) {
            Avance primerAvance = fotos.get(0).getIdAvance();
            datosObra.put("nombreObra", primerAvance.getIdObra().getNombreObra());
            datosObra.put("cooEObra", primerAvance.getIdObra().getCooEObra());
            datosObra.put("cooNObra", primerAvance.getIdObra().getCooNObra());
        }

        datosObra.put("idObra", idObra);
        datosObra.put("totalFotos", fotos.size());
        datosObra.put("fechaGeneracion", new Date().toString());

        // Preparar lista de fotos
        List<Map<String, Object>> listaFotos = new ArrayList<>();

        for (FotoDato fotoDato : fotos) {
            try {
                Map<String, Object> fotoInfo = prepararDatosFotoParaPDF(fotoDato);
                listaFotos.add(fotoInfo);
            } catch (Exception e) {
                // Si hay error con una foto, continuar con las demás
                System.err.println("Error procesando foto " + fotoDato.getIdFotoDato() +
                        ": " + e.getMessage());
            }
        }

        datosObra.put("fotos", listaFotos);
        return datosObra;
    }

    private byte[] enviarAFastAPI(String endpoint, Map<String, Object> datos) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

            // URL de tu servicio FastAPI (ajusta según tu configuración)
            String fastApiUrl = "http://localhost:8000" + endpoint;

            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    fastApiUrl,
                    request,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Error en FastAPI: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error comunicándose con FastAPI: " + e.getMessage(), e);
        }
    }

}