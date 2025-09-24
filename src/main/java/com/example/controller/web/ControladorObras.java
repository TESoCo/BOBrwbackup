package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Obra;
import com.example.servicio.APUServicio;
import com.example.servicio.ObraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/obras")

public class ControladorObras
{
    //Obras
    @Autowired
    private ObraServicio obraServicio;
    //Acá están los métodos para presupuestos
    @GetMapping("/inicioObras")
    public String inicioPresu(Model model){
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("obras",obras);
        return "obras/inicioObra";
    }

    //Agregar nuevo presupuesto
    @GetMapping("/agregarObra")
    public String formAnexarPresupuesto(Model model){
        model.addAttribute("obra", new Obra());
        model.addAttribute("APU", APUServicio.listarElementos());
        return "obras/agregarObra";
    }

    //Función de guardado
    @PostMapping("/salvar")
    public String salvarObra(


            @RequestParam String nombreObra,
            @RequestParam String etapa,
            @RequestParam LocalDate fechaIni,
            @RequestParam LocalDate fechaFin,
            @RequestParam Double cooNObra,
            @RequestParam Double cooEObra,
            @RequestParam List<Long> apuIds, // Changed from actividadIds to apuIds
            @RequestParam List<Double> cantidades) { // Changed from actividadIds to apuIds

        // Create the nuevaObra first to get its ID
        Obra nuevaObra = new Obra();
        nuevaObra.setNombreObra(nombreObra);
        nuevaObra.setEtapa(etapa);
        nuevaObra.setFechaIni(fechaIni);
        nuevaObra.setFechaFin(fechaFin);
        nuevaObra.setCooNObra(cooNObra);
        nuevaObra.setCooEObra(cooEObra);
        obraServicio.salvar(nuevaObra);

        // Validate input sizes match
        if (apuIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("La cantidad de IDs y cantidades no coincide");
        }

        // Add APUs with quantities
        for (int i = 0; i < apuIds.size(); i++) {
            Apu apu = APUServicio.obtenerPorId(apuIds.get(i));
            if (apu != null) {
                // You'll need to implement a method that accepts quantity<-DONE
                obraServicio.agregarApuAObraConCantidad(nuevaObra, apu,cantidades.get(i));
                // For quantities, you'll need to update the junction table<-DONE
            }
        }

    /*
        // Convert to Map<Integer, Double> for JSON storage
        Map<Integer, Double> APUValues = new HashMap<>();
        for (int i = 0; i < apuIds.size(); i++) {
            Integer id = apuIds.get(i);
            Double cantidad = cantidades.get(i);

            if (id == null) {
                throw new IllegalArgumentException("ID de actividad no puede ser nulo");
            }
            if (cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida para actividad ID: " + id);
            }

            APUValues.put(id, cantidad);
        }
*/




        return "redirect:/obras/inicioObra";
    }


    //Función y forma de editado
    @GetMapping("/cambiar/{id_obra}")
    public String cambiarObra(@PathVariable Long id_obra, Model model) {
        Obra obraEditar = obraServicio.localizarObra(id_obra);

        // Create a list of activity IDs and quantities for editing
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(id_obra);
        List<Apu> todosApus = APUServicio.listarElementos();
        List<Double> cantidades = new ArrayList<>();

        model.addAttribute("obraEditar", obraEditar);
        model.addAttribute("apusObra", apusObra);
        model.addAttribute("listApus", obraServicio.listaObra());
        model.addAttribute("Editando", true); // ← This forces EDIT mode
        model.addAttribute("todosApus", todosApus);
        model.addAttribute("matriz", APUServicio.listarElementos());
        //Map<Integer, Double> actividades = obraEditar.getActiviValues();
        //model.addAttribute("actividadIds", actividadIds);
        model.addAttribute("cantidades", cantidades);

/*
        // Create a Map of Material to Quantity
        Map<Apu, Double> listApus = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : obraServicio.obtenerApusPorObra(id_obra)) {
            Apu apuAgregar = APUServicio.obtenerPorId(entry.getKey());
            listApus.put(apuAgregar, entry.getValue());
            apusObra.add(apuAgregar);
            cantidades.add(entry.getValue());
        }
*/

        return "obras/verObras";
    }

    //borrar
    @GetMapping("/borrar/{id_obra}")
    public String borrarObra(Obra obraBorrar) {
        obraServicio.borrar(obraBorrar);
        return "redirect:/obras/inicioObra";
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{id_obra}")
    public String actualizarPresupuesto(
        @PathVariable Long id_obra,
        @RequestParam String obraName,
        @ModelAttribute Obra obraActualizar,
        BindingResult result,
        @RequestParam List<Long> actividadIds,
        @RequestParam List<Double> cantidades,
        Model model) {
        if (result.hasErrors() || actividadIds.isEmpty()) {
            return "redirect:/obras/cambiar/" + id_obra;
        }

        Map<Long, Double> apuValues = new HashMap<>();
        for (int i = 0; i < actividadIds.size(); i++) {
            apuValues.put(actividadIds.get(i), cantidades.get(i));
            obraServicio.agregarApuAObra(obraActualizar, APUServicio.obtenerPorId(actividadIds.get(i)));
        }


        obraActualizar.setNombreObra(obraName);


        obraServicio.actualizar(obraActualizar);
        return "redirect:/obras/inicioObra";
    }

    //Ver obraDetalle en detalle (sólo lectura)
    @GetMapping("/detalle/{id_obra}")
    public String detalleObra(@PathVariable Long id_obra, Model model) {
        Obra obraDetalle = obraServicio.localizarObra(id_obra);
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(id_obra);

/*
        // Create a Map of Material to Quantity
        Map<Apu, Double> listApus = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : obraDetalle.obtenerApusPorObra(id_obra)) {
            Apu verAPUobra = APUServicio.obtenerPorId(entry.getKey());
            listApus.put(verAPUobra, entry.getValue());
        }
*/


        model.addAttribute("obra", obraDetalle);
//        model.addAttribute("listApus", listApus);
        model.addAttribute("Editando", false); // ← This forces VIEW mode
        return "obras/verObras";
    }


    //Materiales (para el manejo de la matriz)
    @Autowired
    private APUServicio APUServicio;

    //Funcionalidad del filtro
    @GetMapping("/filtroPr")
    public String filtroPre(
            @RequestParam(value = "tipoBusqueda", required = false) String tipoBusqueda,
            @RequestParam(value = "valorBusqueda", required = false) String valorBusqueda,
            Integer id_obra,
            Model model) {

        List<Obra> obras = new ArrayList<>(); // Initialize with empty list
        Obra obra = null; // Initialize as null
        String error = null;

        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.isEmpty()) {
            switch (tipoBusqueda) {
                case "idObra":
                    obra = obraServicio.localizarObra(Long.getLong(valorBusqueda));
                    break;
                case "obraName":
                    obras = obraServicio.findByObraNameContaining(valorBusqueda);
                    break;

                default:
                    obras = obraServicio.listaObra();
            }
        } else {
            obras = obraServicio.listaObra();
        }

        model.addAttribute("presupuestos", obras);
        model.addAttribute("presupuesto", obra);

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "obras/inicioObra";
    }

}