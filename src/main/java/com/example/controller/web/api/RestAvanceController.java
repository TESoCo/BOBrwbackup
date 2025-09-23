// RestAvanceController.java
package com.example.controller.web.api;

import com.example.domain.Avance;
import com.example.servicio.AvanceServicio;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/avances")
@CrossOrigin(origins = {"*","http://localhost:3000", "http://localhost:8888", "https://api.apidog.com"})
public class RestAvanceController {

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping
    public ResponseEntity<List<Avance>> getAllAvances() {
        try {
            List<Avance> avances = avanceServicio.listaAvance();
            return ResponseEntity.ok(avances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    @PostMapping
    public ResponseEntity<Avance> createAvance(@RequestBody Avance avance) {
        try {
            avanceServicio.salvar(avance);
            return ResponseEntity.status(HttpStatus.CREATED).body(avance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Avance> updateAvance(@PathVariable Long id, @RequestBody Avance avance) {
        try {
            Avance existingAvance = avanceServicio.localizarAvance(id);
            if (existingAvance == null) {
                return ResponseEntity.notFound().build();
            }
            avance.setIdAvance(id);
            avanceServicio.actualizar(avance);
            return ResponseEntity.ok(avance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvance(@PathVariable Long id) {
        try {
            Avance avance = avanceServicio.localizarAvance(id);
            if (avance == null) {
                return ResponseEntity.notFound().build();
            }
            avanceServicio.borrar(avance);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
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

    @GetMapping("/search/id-matriz/{idMatriz}")
    public ResponseEntity<List<Avance>> searchByIdMatriz(@PathVariable Long idApu) {
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
}