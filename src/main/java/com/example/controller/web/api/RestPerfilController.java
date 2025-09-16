// RestPerfilController.java
package com.example.controller.web.api;

import com.example.domain.Rol;
import com.example.servicio.RolServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/perfiles")
@CrossOrigin(origins = "*")
public class RestPerfilController {

    @Autowired
    private RolServicio rolServicio;

    @GetMapping
    public List<Rol> getAllPerfiles() {
        return rolServicio.listarRoles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rol> getPerfilById(@PathVariable Integer id) {
        Rol rol = rolServicio.buscarPorId(id);
        return rol != null ? ResponseEntity.ok(rol) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Rol createPerfil(@RequestBody Rol rol) {
        rolServicio.guardar(rol);
        return rol;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rol> updatePerfil(@PathVariable Integer id, @RequestBody Rol rol) {
        Rol existing = rolServicio.buscarPorId(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        rol.setIdRol(id);
        rolServicio.guardar(rol);
        return ResponseEntity.ok(rol);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerfil(@PathVariable Integer id) {
        Rol rol = rolServicio.buscarPorId(id);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        // Note: You might need to handle relationships before deletion
        return ResponseEntity.noContent().build();
    }
}