// RestPresupuestoController.java
package com.example.controller.web.api;

import com.example.domain.Obra;
import com.example.servicio.ObraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
@CrossOrigin(origins = "*")
public class RestObraController {

    @Autowired
    private ObraServicio obraServicio;

    @GetMapping
    public List<Obra> getAllObras() {
        return obraServicio.listaObra();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Obra> getObraById(@PathVariable Integer id) {
        Obra obraREST = obraServicio.localizarObra(id);
        return obraREST != null ? ResponseEntity.ok(obraREST) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Obra createObra(@RequestBody Obra nuevaObraREST) {
        obraServicio.salvar(nuevaObraREST);
        return nuevaObraREST;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Obra> updateObra(@PathVariable Integer id, @RequestBody Obra obraActualREST) {
        Obra existing = obraServicio.localizarObra(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        obraActualREST.setIdObra(id);
        obraServicio.actualizar(obraActualREST);
        return ResponseEntity.ok(obraActualREST);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteObra(@PathVariable Integer id) {
        Obra obraBorrar = obraServicio.localizarObra(id);
        if (obraBorrar == null) {
            return ResponseEntity.notFound().build();
        }
        obraServicio.borrar(obraBorrar);
        return ResponseEntity.noContent().build();
    }

    // Search endpoints
    @GetMapping("/search/obra-name/{obraName}")
    public List<Obra> searchByObraName(@PathVariable String obraName) {
        return obraServicio.findByObraName(obraName);
    }

    @GetMapping("/search/obra-name-contains/{obraName}")
    public List<Obra> searchByObraNameContaining(@PathVariable String obraName) {
        return obraServicio.findByObraNameContaining(obraName);
    }

    @GetMapping("/search/obra-name-ignore-case/{obraName}")
    public List<Obra> searchByObraNameIgnoreCase(@PathVariable String obraName) {
        return obraServicio.findByObraNameIgnoreCase(obraName);
    }
}