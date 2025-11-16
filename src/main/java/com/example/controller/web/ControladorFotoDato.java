package com.example.controller.web;

import com.example.domain.Avance;
import com.example.domain.FotoDato;
import com.example.servicio.AvanceServicio;
import com.example.servicio.FotoDatoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

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

            fotoDatoServicio.salvar(fotoDato, fotoFile);

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

                fotoDatoServicio.salvar(fotoDato, archivoFoto);
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
}