package com.example.controller.web;

import com.example.dao.UsuarioDao;
import com.example.domain.*;
import com.example.servicio.*;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/avances")

public class ControladorAvance
{
    //Servicios para utilizar
    @Autowired
    private AvanceServicio avanceServicio;
    @Autowired
    private ObraServicio obraServicio;
    @Autowired
    private APUServicio apuServicio;
    @Autowired
    private UsuarioServicio usuarioServicio; // Use the interface, not implementation


    //Acá están los métodos
    @GetMapping("/inicioAvances")
    public String inicioAvance(
           // @RequestParam(required = false) String obraName,
           @RequestParam(required = false) Integer idObraTexto,
           @RequestParam(required = false) Integer idObraSelect,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) Integer idAPU,
            @RequestParam(required = false) String fecha,
            Model model){


        //Necesito cargar obras para mostrar nombres
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("presupuestos", obras);

//Este if es para las búsquedas por ID

// Start with all avances
        List<Avance> avances = avanceServicio.listaAvance();

        // Apply filters in a more flexible way
        if (idObraSelect != null) {
            avances = avanceServicio.buscarPorIdObra(idObraSelect);
        }
        if (idObraTexto != null) {
            avances = avanceServicio.buscarPorIdObra(idObraTexto);
        }
        if (idUsuario != null && !idUsuario.isEmpty()) {
            avances = avances.stream()
                    .filter(a -> a.getIdUsuario().equals(idUsuario))
                    .collect(Collectors.toList());
        }
        if (fecha != null && !fecha.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(fecha);
                avances = avances.stream()
                        .filter(a -> a.getFechaAvance() != null && a.getFechaAvance().equals(filterDate))
                        .collect(Collectors.toList());
            } catch (DateTimeParseException e) {
                // Handle invalid date format
                model.addAttribute("error", "Formato de fecha inválido");
            }
        }

        // Add the filtered results and parameters back to the model
        model.addAttribute("avances", avances);
        model.addAttribute("idObraSelect", idObraSelect);
        model.addAttribute("idObraTexto", idObraTexto);
        model.addAttribute("idUsuario", idUsuario);
        model.addAttribute("fecha", fecha);

        return "avances/inicioAvances";
    }

    //Agregar nuevo
    @GetMapping("/agregarAvance")
    public String formAnexarAvance(Model model){
        List<Obra> obras = obraServicio.listaObra();
        List<Apu> matriz = APUServicio.listarElementos();

        model.addAttribute("avance", new Avance());
        model.addAttribute("obras",obras);
        model.addAttribute("matriz", matriz);
        return "avances/agregarAvance";
    }

    //Función de guardado
    @PostMapping("/salvar")
    public String salvarAvance(
            //Authentication auth, // Add this parameter to get the logged-in user
            @RequestParam Integer idUsuario,
            @RequestParam Integer idObra,
            @RequestParam String fecha,
            @RequestParam Integer idApu,
            @RequestParam Double cantidad) {

        // Get the currently authenticated user
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // For now, let's just print it to verify it works
        System.out.println("Logged in username: " + username);

        // Load the full user object from database
        Usuario usuarioLogeado = usuarioServicio.encontrarPorId(idUsuario) ;


        Avance avance = new Avance();
        avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario) );
        avance.setIdObra(obraServicio.localizarObra(idObra));
        avance.setFechaAvance(LocalDate.parse(fecha));
        avance.setApu(APUServicio.obtenerPorId(idApu));
        avance.setCantEjec(cantidad);


        avanceServicio.salvar(avance);
        return "redirect:/avances/inicioAvances";
    }


    //Función y forma de editado
    /*@GetMapping("/cambiar/{id_avance}")
    public String cambiarAvance(@PathVariable Integer id_avance, Model model) {
        Avance avance = avanceServicio.localizarAvance(id_avance);

        model.addAttribute("avance", avance);
        model.addAttribute("Actividad", avanceServicio.localizarAvance(id_avance));
        model.addAttribute("Editando", true); // ← This forces EDIT mode
        model.addAttribute("matriz", matrizServicio.listarElementos());

        return "avances/verAvances";
    }*/


    //borrar
    @GetMapping("/borrar/{idAvance}")
    public String borrarAvance(Avance avance) {
        avanceServicio.borrar(avance);
        return "redirect:/avances/inicioAvances";
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idAvance}")
    public String actualizarPresupuesto(
        Authentication auth, // Add this parameter to get the logged-in user
        @PathVariable Integer idAvance,
        @ModelAttribute Avance avance,
        @RequestParam Double cantidad,
        @RequestParam Integer idUsuario,
        @RequestParam Integer idObra,
        @RequestParam String fecha,
        BindingResult result,
        @RequestParam Integer idApu,
        Model model) {
        if (result.hasErrors()) {
            return "redirect:/avances/cambiar/" + idAvance;
        }

        // Get the username from the authentication object
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        avance.setIdUsuario(usuarioServicio.encontrarPorId(idUsuario));
        avance.setIdObra(obraServicio.localizarObra(idObra));
        avance.setFechaAvance(LocalDate.parse(fecha));
        avance.setApu(APUServicio.obtenerPorId(idApu));
        avance.setCantEjec(cantidad);


        avanceServicio.actualizar(avance);
        return "redirect:/avances/inicioAvances";
    }

    //Ver detalle (sólo lectura)
    @GetMapping("/detalle/{idAvance}")
    public String detalleAvance(@PathVariable Integer idAvance, Model model) {
        Avance avance = avanceServicio.localizarAvance(idAvance);
        List<Apu> matriz = APUServicio.listarElementos();
        List<Obra> obras = obraServicio.listaObra();

        model.addAttribute("avance", avance);
        model.addAttribute("actividad", avanceServicio.localizarAvance(idAvance));
        model.addAttribute("obras",obras);
        model.addAttribute("matriz", matriz);
        model.addAttribute("Editando", false); // ← This forces VIEW mode
        return "avances/verAvances";
    }




    //Materiales (para el manejo de la matriz)
    @Autowired
    private APUServicio APUServicio;



}