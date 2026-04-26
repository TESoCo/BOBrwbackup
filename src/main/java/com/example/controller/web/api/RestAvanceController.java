// RestAvanceController.java
package com.example.controller.web.api;

import com.example.domain.*;
import com.example.dto.AvanceRequestDTO;
import com.example.servicio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/avances")
@CrossOrigin(origins = {"*","http://localhost:3000", "http://localhost:8888", "https://api.apidog.com"})
public class RestAvanceController {

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private APUServicio apuServicio;

    @Autowired
    private FotoDatoServicio fotoDatoServicio;

    //Buscar avances desde la internet
    @GetMapping
    public ResponseEntity<List<Avance>> getAllAvances() {
        try {
            List<Avance> avances = avanceServicio.listaAvance();
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    //Crear avances desde la internet
    /**
     * Crear un nuevo avance
     * POST /api/avances
     *
     * Body esperado (JSON):
     * {
     *   "idObra": { "idObra": 1 },
     *   "idUsuario": { "idUsuario": 1 },
     *   "idApu": { "idAPU": 1 },
     *   "fechaAvance": "2024-01-15",
     *   "cantEjec": 10.5
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAvance(@RequestBody AvanceRequestDTO request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extraer los ID del objeto petición
            Long idObra = request.getIdObra();
            Long idUsuario = request.getIdUsuario();
            Long idApu = request.getIdApu();
            LocalDate fecha = request.getFechaAvance();
            Double cantEjec = request.getCantEjec();

            // Validaciones
            if (idObra == null || idUsuario == null || idApu == null || fecha == null) {
                response.put("success", false);
                response.put("error", "Faltan campos requeridos: idObra, idUsuario, idApu, fechaAvance, cantEjec");
                return ResponseEntity.badRequest().body(response);
            }

            // Buscar las entidades relacionadas
            Obra obra = obraServicio.localizarObra(idObra);
            if (obra == null) {
                response.put("success", false);
                response.put("error", "Obra no encontrada con ID: " + idObra);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Usuario usuario = usuarioServicio.encontrarPorId(idUsuario);
            if (usuario == null) {
                response.put("success", false);
                response.put("error", "Usuario no encontrado con ID: " + idUsuario);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Apu apu = apuServicio.obtenerPorId(idApu);
            if (apu == null) {
                response.put("success", false);
                response.put("error", "APU no encontrado con ID: " + idApu);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Crear y guardar el avance
            Avance avance = new Avance();
            avance.setIdObra(obra);
            avance.setIdUsuario(usuario);
            avance.setIdApu(apu);
            avance.setFechaAvance(fecha);
            avance.setCantEjec(cantEjec);
            avance.setAnular(false);  // Por defecto no anulado

            avanceServicio.salvar(avance);

            response.put("success", true);
            response.put("message", "Avance creado correctamente");
            response.put("avanceId", avance.getIdAvance());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    //Obtener información de avance
    @GetMapping("/{id}")
    public ResponseEntity<Avance> getAvanceById(@PathVariable Long id) {
        try {
            Avance avance = avanceServicio.localizarAvance(id);
            if (avance != null) {
                return ResponseEntity.ok(avance);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //Actualizar avance
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAvance(@PathVariable Long id, @RequestBody AvanceRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Avance existingAvance = avanceServicio.localizarAvance(id);
            if (existingAvance == null) {
                response.put("success", false);
                response.put("error", "Avance no encontrado");
                return ResponseEntity.notFound().build();
            }

            // Actualizar solo los campos que NO son null en el request
            if (request.getCantEjec() != null) {
                existingAvance.setCantEjec(request.getCantEjec());
            }

            if (request.getFechaAvance() != null) {
                existingAvance.setFechaAvance(request.getFechaAvance());
            }

            avanceServicio.actualizar(existingAvance);

            response.put("success", true);
            response.put("message", "Avance actualizado correctamente");
            response.put("avanceId", existingAvance.getIdAvance());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //Borrar  avance
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>>  deleteAvance(@PathVariable Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            Avance avance = avanceServicio.localizarAvance(id);
            if (avance == null) {
                response.put("success", false);
                response.put("error", "Avance no encontrado");
                return ResponseEntity.notFound().build();
            }
            avanceServicio.borrar(avance);

            response.put("success", true);
            response.put("message", "Avance eliminado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // Search endpoints
    @GetMapping("/search/id-usuario/{idUsuario}")
    public ResponseEntity<List<Avance>> searchByIdUsuario(@PathVariable Long idUsuario) {
        try {
            List<Avance> avances = avanceServicio.buscarPorIdUsuario(idUsuario);
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/id-obra/{idObra}")
    public ResponseEntity<List<Avance>> searchByIdObra(@PathVariable Long idObra) {
        try {
            List<Avance> avances = avanceServicio.buscarPorIdObra(idObra);
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/id-Apu/{idApu}")
    public ResponseEntity<List<Avance>> searchByIdApu(@PathVariable Long idApu) {
        try {
            List<Avance> avances = avanceServicio.buscarPorIdApu(idApu);
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/fecha/{fecha}")
    public ResponseEntity<List<Avance>> searchByFecha(@PathVariable String fecha) {
        try {
            List<Avance> avances = avanceServicio.buscarPorFecha(LocalDate.parse(fecha));
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GetMapping("/search/usuario-fecha")
    public ResponseEntity<List<Avance>> searchByUsuarioAndFecha(
            @RequestParam Long idUsuario,
            @RequestParam String fecha) {
        try {
            List<Avance> avances = avanceServicio.buscarPorUsuarioYFecha(usuarioServicio.encontrarPorId(idUsuario), fecha);
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // MÉTODOS PARA GESTIÓN DE FOTOS (FOTODATO)


    /**
     * Agregar foto a un avance existente
     * POST /api/avances/{idAvance}/fotos
     *
     * @param idAvance ID del avance
     * @param archivo Archivo de imagen (multipart/form-data)
     * @param latitud Latitud (opcional)
     * @param longitud Longitud (opcional)
     */
    @PostMapping("/{idAvance}/fotos")
    public ResponseEntity<Map<String, Object>> agregarFoto(
            @PathVariable Long idAvance,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Verificar que el avance existe
            Avance avance = avanceServicio.localizarAvance(idAvance);
            if (avance == null) {
                response.put("success", false);
                response.put("error", "Avance no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Crear objeto FotoDato
            FotoDato foto = new FotoDato();
            foto.setIdAvance(avance);
            foto.setCooNFoto(latitud);
            foto.setCooEFoto(longitud);
            foto.setFechaFoto(LocalDate.now());

            // Guardar usando el servicio existente (que maneja GridFS)
            fotoDatoServicio.salvar(foto, archivo);

            response.put("success", true);
            response.put("fotoId", foto.getIdFotoDato());
            response.put("message", "Foto agregada correctamente");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtener todas las fotos de un avance
     * GET /api/avances/{idAvance}/fotos
     */
    @GetMapping("/{idAvance}/fotos")
    public ResponseEntity<Map<String, Object>> getFotosByAvance(@PathVariable Long idAvance) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<FotoDato> fotos = fotoDatoServicio.buscarPorIdAvance(idAvance);

            response.put("success", true);
            response.put("count", fotos.size());
            response.put("fotos", fotos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Descargar / ver una foto específica
     * GET /api/avances/fotos/{idFoto}/download
     */
    @GetMapping("/fotos/{idFoto}/download")
    public ResponseEntity<?> downloadFoto(@PathVariable Long idFoto) {

        try {
            FotoDato foto = fotoDatoServicio.localizarFotoDato(idFoto);

            if (foto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Foto no encontrada"));
            }

            // Obtener los bytes de la imagen desde GridFS
            byte[] imagen = fotoDatoServicio.obtenerArchivoFoto(foto.getGridfsFileId());

            // Determinar el tipo de contenido
            String contentType = foto.getTipoMime();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "inline; filename=\"" + foto.getNombreArchivo() + "\"")
                    .body(imagen);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar una foto de un avance
     * DELETE /api/avances/fotos/{idFoto}
     */
    @DeleteMapping("/fotos/{idFoto}")
    public ResponseEntity<Map<String, Object>> deleteFoto(@PathVariable Long idFoto) {

        Map<String, Object> response = new HashMap<>();

        try {
            FotoDato foto = fotoDatoServicio.localizarFotoDato(idFoto);

            if (foto == null) {
                response.put("success", false);
                response.put("error", "Foto no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            fotoDatoServicio.borrar(foto);

            response.put("success", true);
            response.put("message", "Foto eliminada correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}