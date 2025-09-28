package com.example.controller.web;

import com.example.domain.Apu;
import com.example.domain.Obra;
import com.example.servicio.APUServicio;
import com.example.servicio.ObraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/obras")

public class ControladorObras
{
    //Obras
    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private APUServicio apuServicio;



    //Acá están los métodos para presupuestos
    @GetMapping("/inicioObra")
    public String inicioObra(Model model, Authentication authentication){
        List<Obra> obras = obraServicio.listaObra();
        model.addAttribute("obras",obras);


        //INFORMACION DE USUARIO PARA HEADER Y PERMISOS
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            // Debug información del usuario
            System.out.println("Usuario autenticado: " + username);
            System.out.println("Autoridades: " + authorities);

            // Agregar información específica del usuario al modelo
            model.addAttribute("nombreUsuario", username);
            model.addAttribute("autoridades", authorities);

            // Verificar roles específicos
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);
        }

        //FILTRAR OBRAS CON COORDENADAS PARA EL MAPA
        // Filtrar obras que tengan coordenadas (opcional)
        List<Obra> obrasConCoordenadas = obras.stream()
                .filter(obra -> obra.getCooNObra() != null && obra.getCooEObra() != null)
                .collect(Collectors.toList());

        model.addAttribute("obras", obrasConCoordenadas);
        model.addAttribute("fotoDatos", new ArrayList<>()); // o tu lista real de fotos
        // TODO: conexion con fotodato

        return "obras/inicioObra";
    }

    //Agregar nuevo presupuesto
    @GetMapping("/agregarObra")
    public String formAnexarPresupuesto(Model model){
        model.addAttribute("obra", new Obra());
        model.addAttribute("APUs", APUServicio.listarElementos());

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
            @RequestParam List<Double> cantidades, // Changed from actividadIds to apuIds
            RedirectAttributes redirectAttributes) {
        // Create the nuevaObra first to get its ID
        Obra nuevaObra = new Obra();
        //no repetir nombres de obras
        List<Obra> obrasExt = obraServicio.findByObraName(nombreObra);
        if (obrasExt!=null&&!obrasExt.isEmpty())
        {
            for(Obra obraExistente:obrasExt)
            {
                if(Objects.equals(obraExistente.getNombreObra(), nombreObra))
                {
                    redirectAttributes.addFlashAttribute("error", "El nombre de obra ya existe");
                    return "redirect:obras/agregarObra";
                }
            }
        }


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

        return "redirect:inicioObra";
    }


    //Función y forma de editado
    @GetMapping("/cambiar/{idObra}")
    public String cambiarObra(@PathVariable Long id_obra, Model model) {
        Obra obra = obraServicio.localizarObra(id_obra);

        // Create a list of activity IDs and quantities for editing
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(id_obra);
        List<Apu> todosApus = APUServicio.listarElementos();
        List<Double> cantidades = new ArrayList<>();

        model.addAttribute("obra", obra);
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
    /*@GetMapping("/borrar/{id_obra}")
    public String borrarObra(Obra obraBorrar) {
        obraServicio.borrar(obraBorrar);
        return "redirect:/obras/inicioObra";
    }*/

    //anular

    @GetMapping("/anular/{idObra}")
    public String anularObra(Obra obraAnular)
    {
        obraAnular.setAnular(true);
        return "redirect:obras/inicioObra";
    }

    //funcionalidad para guardar cambios
    @PostMapping("/actualizar/{idObra}")
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
        return "redirect:obras/inicioObra";
    }

    //Ver obraDetalle en detalle (sólo lectura)
    @GetMapping("/detalle/{idObra}")
    public String detalleObra(@PathVariable Long idObra, Model model) {
        Obra obra = obraServicio.localizarObra(idObra);
        List<Apu> apusObra = obraServicio.obtenerApusEntidadesPorObra(idObra);

        List<Double> valApusObra = new ArrayList<>();
        if(apusObra!=null&& !apusObra.isEmpty())
        {
            for(Apu apu : apusObra)
            {
                valApusObra.add(apuServicio.vTotalAPU(apu));
            }
        }

        /*
        // Create a Map of Material to Quantity
        Map<Apu, Double> listApus = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : obra.obtenerApusPorObra(id_obra)) {
            Apu verAPUobra = APUServicio.obtenerPorId(entry.getKey());
            listApus.put(verAPUobra, entry.getValue());
        }
        */

        model.addAttribute("obra", obra);
        model.addAttribute("valApus",valApusObra);
        model.addAttribute("apusObra",apusObra);
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

            Model model) {

        List<Obra> obras = new ArrayList<>(); // Initialize with empty list
        Obra obra = null; // Initialize as null
        String error = null;

        if (tipoBusqueda != null && valorBusqueda != null && !valorBusqueda.isEmpty()) {
            switch (tipoBusqueda) {
                case "idObra":
                    Long id = Long.parseLong(valorBusqueda);
                    obra = obraServicio.localizarObra(id);
                    if (obra != null) {
                        obras.add(obra);
                    }
                    break;
                case "nombreObra":
                    obras = obraServicio.findByObraNameContaining(valorBusqueda);
                    break;

                default:
                    obras = obraServicio.listaObra();
            }
        } else {
            obras = obraServicio.listaObra();
        }

        model.addAttribute("obras", obras);
        model.addAttribute("obra", obra);

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "obras/inicioObra";
    }

}