// RestInventarioController.java
package com.example.controller.web.api;

import com.example.domain.Inventario;
import com.example.servicio.InventarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
@CrossOrigin(origins = "*")
public class RestInventarioController {

    @Autowired
    private InventarioServicio inventarioServicio;

    @GetMapping
    public List<Inventario> getAllInventarios() {
        return inventarioServicio.listaInventarios();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventario> getInventarioById(@PathVariable Long id) {
        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        return inventario != null ? ResponseEntity.ok(inventario) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Inventario createInventario(@RequestBody Inventario inventario) {
        inventarioServicio.guardarInv(inventario);
        return inventario;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inventario> updateInventario(@PathVariable Long id, @RequestBody Inventario inventario) {
        Inventario existing = inventarioServicio.localizarInventarioPorId(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        inventario.setIdInventario(id);
        inventarioServicio.cambiarInv(inventario);
        return ResponseEntity.ok(inventario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventario(@PathVariable Long id) {
        Inventario inventario = inventarioServicio.localizarInventarioPorId(id);
        if (inventario == null) {
            return ResponseEntity.notFound().build();
        }
        inventarioServicio.borrarInv(inventario);
        return ResponseEntity.noContent().build();
    }

    // Search endpoints
    @GetMapping("/search/nombre-gestor/{nombreGestor}")
    public List<Inventario> searchByNombreGestor(@PathVariable String nombreGestor) {
        return inventarioServicio.buscarPorNombreGestor(nombreGestor);
    }

    @GetMapping("/search/nombre-obra/{nombreObra}")
    public List<Inventario> searchByNombreObra(@PathVariable String nombreObra) {
        return inventarioServicio.buscarPorNombreObra(nombreObra);
    }

    @GetMapping("/search/fecha/{fecha}")
    public List<Inventario> searchByFecha(@PathVariable String fecha) {
        return inventarioServicio.buscarPorFecha(fecha);
    }
}