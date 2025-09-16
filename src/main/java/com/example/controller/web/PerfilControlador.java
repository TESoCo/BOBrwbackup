package com.example.controller.web;



import com.example.domain.Rol;
import com.example.domain.Permiso;
import com.example.servicio.RolServicio;
import com.example.servicio.PermisoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/perfiles")
public class PerfilControlador {

    @Autowired
    private RolServicio rolServicio;

    @Autowired
    private PermisoServicio permisoServicio;

    @GetMapping("/{id}/permisos")
    public String verPermisos(@PathVariable Integer id, Model model){
        Rol rol = rolServicio.buscarPorId(id);
        List<Permiso> todos = permisoServicio.listarPermisos();

        model.addAttribute("perfil", rol);
        model.addAttribute("todosPermisos", todos);
        return "asignar_permisos";
    }

    @PostMapping("/{id}/permisos")
    public String actualizarPermisos(@PathVariable Integer id,
                                     @RequestParam List<Long> permisosIds){
        Rol rol = rolServicio.buscarPorId(id);
        List<Permiso> permisosSeleccionados = permisoServicio.listarPermisos()
                .stream()
                .filter(p -> permisosIds.contains(p.getId()))
                .toList();
        rol.setPermisos(permisosSeleccionados);
        rolServicio.guardar(rol);

        return "redirect:/perfiles";

    }
}
