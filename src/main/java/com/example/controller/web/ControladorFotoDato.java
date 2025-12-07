package com.example.controller.web;

import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.servicio.AvanceServicio;
import com.example.servicio.FotoDatoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // Variable de entorno para la URL del servicio FastAPI
    @Value("${fastapi.url:http://localhost:8000}")
    private String fastApiUrl;

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

    // 1. PDF de una sola foto CON INFORMACIÓN
    @GetMapping("/pdf/foto/{idFotoDato}")
    @ResponseBody
    public ResponseEntity<byte[]> generarPDFFoto(@PathVariable Long idFotoDato) {
        try {
            // Obtener FotoDato
            FotoDato fotoDato = fotoDatoServicio.localizarFotoDato(idFotoDato);
            if (fotoDato == null || fotoDato.getGridfsFileId() == null) {
                return ResponseEntity.notFound().build();
            }

            // Preparar información de la foto
            Map<String, Object> fotoInfo = prepararInfoFotoParaFastAPI(fotoDato);

            // Crear lista con una sola foto
            List<Map<String, Object>> fotosList = new ArrayList<>();
            fotosList.add(fotoInfo);

            // Crear request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fotos", fotosList);

            // Enviar a FastAPI
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Usar la variable de entorno para construir la URL
            String pdfEndpoint = fastApiUrl + "/pdf/fotos-con-info";

            ResponseEntity<byte[]> response = restTemplate.postForEntity(pdfEndpoint, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] pdfBytes = response.getBody();

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_PDF);

                String nombreArchivo = "foto_" + idFotoDato;
                if (fotoDato.getNombreArchivo() != null) {
                    nombreArchivo = fotoDato.getNombreArchivo().replaceAll("[^a-zA-Z0-9.]", "_");
                }

                responseHeaders.setContentDispositionFormData("filename",
                        nombreArchivo + ".pdf");

                return new ResponseEntity<>(pdfBytes, responseHeaders, HttpStatus.OK);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("Error generando PDF desde FastAPI").getBytes());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando PDF: " + e.getMessage()).getBytes());
        }
    }

    // 2. PDF de todas las fotos de una obra CON INFORMACIÓN
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

            // Preparar la lista de fotos con toda la información
            List<Map<String, Object>> fotosConInfo = new ArrayList<>();

            for (FotoDato foto : fotos) {
                if (foto.getGridfsFileId() != null) {
                    Map<String, Object> fotoInfo = prepararInfoFotoParaFastAPI(foto);
                    fotosConInfo.add(fotoInfo);
                }
            }

            if (fotosConInfo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No hay fotos con IDs de GridFS válidos".getBytes());
            }

            // Crear el request body para FastAPI
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fotos", fotosConInfo);

            // Enviar a FastAPI
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String pdfEndpoint = fastApiUrl + "/pdf/fotos-con-info";
            ResponseEntity<byte[]> response = restTemplate.postForEntity(pdfEndpoint, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] pdfBytes = response.getBody();

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_PDF);

                // Nombre del archivo
                String nombreObra = "obra_" + idObra;
                if (!fotos.isEmpty() && fotos.get(0).getIdAvance() != null
                        && fotos.get(0).getIdAvance().getIdObra() != null) {
                    nombreObra = fotos.get(0).getIdAvance().getIdObra().getNombreObra()
                            .replaceAll("[^a-zA-Z0-9]", "_");
                }

                responseHeaders.setContentDispositionFormData("filename",
                        "reporte_completo_" + nombreObra + ".pdf");

                return new ResponseEntity<>(pdfBytes, responseHeaders, HttpStatus.OK);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("Error generando PDF desde FastAPI").getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
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

            // Construir URL completa usando la variable de entorno
            String fullUrl = fastApiUrl + endpoint;

            // Si la URL contiene {file_id}, reemplazarlo con el valor de datos
            if (fastApiUrl.contains("{file_id}") && datos.containsKey("gridfsFileId")) {
                fastApiUrl = fastApiUrl.replace("{file_id}", datos.get("gridfsFileId").toString());
            }

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

    private Map<String, Object> prepararInfoFotoParaFastAPI(FotoDato foto) {
        Map<String, Object> fotoInfo = new HashMap<>();

        // Información básica
        fotoInfo.put("gridfs_id", foto.getGridfsFileId());
        fotoInfo.put("nombre_archivo", foto.getNombreArchivo());

        // Fecha
        if (foto.getFechaFoto() != null) {
            fotoInfo.put("fecha_foto", foto.getFechaFoto().toString());
        }

        // Coordenadas
        if (foto.getCooNFoto() != null) {
            fotoInfo.put("coordenadas_n", foto.getCooNFoto());
        }
        if (foto.getCooEFoto() != null) {
            fotoInfo.put("coordenadas_e", foto.getCooEFoto());
        }

        // Información del avance (si existe)
        if (foto.getIdAvance() != null) {
            Avance avance = foto.getIdAvance();

            // Información de la obra
            if (avance.getIdObra() != null) {
                fotoInfo.put("nombre_obra", avance.getIdObra().getNombreObra());
            }

            // Información del usuario
            if (avance.getIdUsuario() != null) {
                fotoInfo.put("nombre_usuario", avance.getIdUsuario().getNombreUsuario());
            }

            // Información de la actividad (APU)
            if (avance.getIdApu() != null) {
                fotoInfo.put("descripcion_actividad", avance.getIdApu().getNombreAPU());
            }

            // Información adicional del avance
            fotoInfo.put("id_avance", avance.getIdAvance());
            if (avance.getFechaAvance() != null) {
                fotoInfo.put("fecha_avance", avance.getFechaAvance().toString());
            }
            if (avance.getCantEjec() != null) {
                fotoInfo.put("cantidad_ejecutada", avance.getCantEjec());
            }
        }

        // Información adicional del FotoDato
        fotoInfo.put("id_fotodato", foto.getIdFotoDato());
        fotoInfo.put("tamanio_archivo", foto.getTamanioArchivo());
        fotoInfo.put("tipo_mime", foto.getTipoMime());

        return fotoInfo;
    }

}