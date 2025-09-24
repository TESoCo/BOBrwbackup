package com.example.controller.web;

import com.example.domain.*;
import com.example.servicio.AvanceServicio;
import com.example.servicio.InventarioServicio;
import com.example.servicio.ObraServicio;
import org.apache.poi.hpsf.Decimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ControladorDashboard {

    @Autowired
    private ObraServicio obraServicio;

    @Autowired
    private AvanceServicio avanceServicio;

    @Autowired
    private InventarioServicio inventarioServicio;



    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        try {
            // Obtener datos reales de obras
            List<Obra> obras = obraServicio.listaObra();
            int cantidadObras = obras.size();

            // Calcular valor total de obras (sumando valores de APUs)
            BigDecimal valorTotalObras = calcularValorTotalObras(obras);

            // Obtener avances recientes (últimos 30 días)
            List<Avance> avancesRecientes = obtenerAvancesRecientes();

            // Obtener inventarios recientes (últimos 30 días)
            List<Inventario> inventariosRecientes = obtenerInventariosRecientes();

            // Obtener obras para el mapa
            List<Obra> obrasConCoordenadas = obtenerObrasConCoordenadas(obras);


            // Agregar datos al modelo
            model.addAttribute("cantidadObras", cantidadObras);
            model.addAttribute("valorTotalObras", valorTotalObras);
            model.addAttribute("obras", obras);
            model.addAttribute("avancesRecientes", avancesRecientes);
            model.addAttribute("inventariosRecientes", inventariosRecientes);
            model.addAttribute("hayObrasConCoordenadas", obrasConCoordenadas != null && !obrasConCoordenadas.isEmpty());
            model.addAttribute("totalAvances", avancesRecientes != null ? avancesRecientes.size() : 0);
            model.addAttribute("totalInventarios", inventariosRecientes != null ? inventariosRecientes.size() : 0);

            // Debug: Verificar roles del usuario actual
            if (authentication != null && authentication.isAuthenticated()) {
                System.out.println("Usuario: " + authentication.getName());
                System.out.println("Autoridades: " + authentication.getAuthorities());

                // Debug information
                System.out.println("Total obras: " + cantidadObras);
                System.out.println("Obras con coordenadas: " + (obrasConCoordenadas != null ? obrasConCoordenadas.size() : 0));
                if (obrasConCoordenadas != null && !obrasConCoordenadas.isEmpty()) {
                    obrasConCoordenadas.forEach(obra ->
                            System.out.println("Obra: " + obra.getNombreObra() +
                                    " - Coords: " + obra.getCooNObra() + ", " + obra.getCooEObra()));
                }
            }
        }
        catch (Exception e){
                System.err.println("Error en dashboard: " + e.getMessage());
                e.printStackTrace();

            // Valores por defecto en caso de error
            model.addAttribute("cantidadObras", 0);
            model.addAttribute("valorTotalObras", BigDecimal.ZERO);
            model.addAttribute("obras", new ArrayList<>());
            model.addAttribute("avancesRecientes", new ArrayList<>());
            model.addAttribute("inventariosRecientes", new ArrayList<>());
            model.addAttribute("totalAvances", 0);
            model.addAttribute("totalInventarios", 0);
            model.addAttribute("estadisticasAvance", new ArrayList<>());
            model.addAttribute("hayObrasConCoordenadas", false);
        }




            // Agregar información de roles al modelo para debugging
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            boolean isSupervisor = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERVISOR"));
            boolean isOperativo = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_OPERATIVO"));

            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSupervisor", isSupervisor);
            model.addAttribute("isOperativo", isOperativo);


        return "dashboard";
    }

    //Valor total de las obras sumando valores de los apus de todas las obras
    private BigDecimal calcularValorTotalObras(List<Obra> obras) {
        BigDecimal total = BigDecimal.ZERO;
        for (Obra obra : obras) {
            List<ApusObra> apusObra = obra.getApusObraList();
            for (ApusObra apuObra : apusObra) {
                Apu apu = apuObra.getApu();
                if (apu != null) {
                    BigDecimal valorApu = BigDecimal.ZERO;

                    // Sumar valores del APU
                    if (apu.getVMaterialesAPU() != null) {
                        valorApu = valorApu.add(apu.getVMaterialesAPU());
                    }
                    if (apu.getVManoDeObraAPU() != null) {
                        valorApu = valorApu.add(apu.getVManoDeObraAPU());
                    }
                    if (apu.getVTransporteAPU() != null) {
                        valorApu = valorApu.add(apu.getVTransporteAPU());
                    }
                    if (apu.getVMiscAPU() != null) {
                        valorApu = valorApu.add(apu.getVMiscAPU());
                    }

                    // Multiplicar por la cantidad en la obra
                    BigDecimal cantidad = BigDecimal.valueOf(apuObra.getCantidad());
                    total = total.add(valorApu.multiply(cantidad));
                }
            }
        }
        return total;
    }

    private List<Avance> obtenerAvancesRecientes() {
        // Obtener avances de los últimos 30 días
        LocalDate fechaLimite = LocalDate.now().minusDays(30);
        List<Avance> todosAvances = avanceServicio.listaAvance();

        return todosAvances.stream()
                .filter(avance -> avance.getFechaAvance() != null &&
                        avance.getFechaAvance().isAfter(fechaLimite))
                .sorted((a1, a2) -> a2.getFechaAvance().compareTo(a1.getFechaAvance()))
                .limit(10) // Limitar a 10 más recientes
                .collect(Collectors.toList());
    }

    private List<Inventario> obtenerInventariosRecientes() {
        // Obtener inventarios de los últimos 30 días
        LocalDate fechaLimite = LocalDate.now().minusDays(30);
        List<Inventario> todosInventarios = inventarioServicio.listaInventarios();

        return todosInventarios.stream()
                .filter(inv -> inv.getFechaIngreso() != null &&
                        inv.getFechaIngreso().isAfter(fechaLimite))
                .sorted((i1, i2) -> i2.getFechaIngreso().compareTo(i1.getFechaIngreso()))
                .limit(10) // Limitar a 10 más recientes
                .collect(Collectors.toList());
    }



    private List<Obra> obtenerObrasConCoordenadas(List<Obra> obras) {
        if (obras == null) {
            return new ArrayList<>();
        }
        return obras.stream()
                .filter(obra -> obra != null &&
                        obra.getCooNObra() != null &&
                        obra.getCooEObra() != null)
                .collect(Collectors.toList());
    }

    private List<Object[]> obtenerEstadisticasAvance(List<Obra> obras) {
        if (obras == null || obras.isEmpty()) {
            return new ArrayList<>();
        }

        return obras.stream()
                .filter(obra -> obra != null && obra.getNombreObra() != null)
                .map(obra -> {
                    double porcentajeAvance = calcularPorcentajeAvance(obra);
                    return new Object[]{obra.getNombreObra(), porcentajeAvance};
                })
                .collect(Collectors.toList());
    }

    private double calcularPorcentajeAvance(Obra obra) {
        if (obra == null) return 0.0;

        try {
            // Calcular porcentaje de avance basado en los avances registrados
            List<Avance> avancesObra = avanceServicio.buscarPorIdObra(obra.getIdObra());

            if (avancesObra == null || avancesObra.isEmpty()) return 0.0;

            double totalEjecutado = avancesObra.stream()
                    .mapToDouble(avance -> avance.getCantEjec() != null ? avance.getCantEjec() : 0.0)
                    .sum();

            double totalPresupuestado = 0.0;
            if (obra.getApusObraList() != null) {
                totalPresupuestado = obra.getApusObraList().stream()
                        .mapToDouble(apuObra -> apuObra.getCantidad() != null ? apuObra.getCantidad() : 0.0)
                        .sum();
            }

            if (totalPresupuestado == 0) return 0.0;

            return (totalEjecutado / totalPresupuestado) * 100;
        } catch (Exception e) {
            System.err.println("Error calculando porcentaje de avance: " + e.getMessage());
            return 0.0;
        }
    }



    @GetMapping("/redirigir")
    public String redirectToDashboard() {
        // Since your dashboard uses Thymeleaf security tags to show/hide content
        // based on permissions, you can simply redirect everyone to the dashboard
        // and let the frontend handle what they can see

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Optional: Log the redirection for debugging
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

            System.out.println("User " + username + " with roles " + authorities + " redirected to dashboard");
        }

        return "redirect:/dashboard";
    }
}