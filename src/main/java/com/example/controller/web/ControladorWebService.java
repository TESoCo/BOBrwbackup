package com.example.controller.web;

import com.example.domain.Avance;
import com.example.servicioWeb.BOBWS;
import jakarta.xml.ws.Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.xml.namespace.QName;

import java.net.URL;
import java.util.List;

@Controller
public class ControladorWebService
{
    @GetMapping("/buscarAvance")
    public String mostrarBusqueda() {
        return "buscarAvance";
    }

    @PostMapping("/buscarAvance")
    public String buscarAvance(@RequestParam Long idObra, Model model) {
        try {
            URL url = new URL("http://localhost:8080/BOBWS?wsdl");
            QName qname = new QName("http://ws.example.com/", "BOBWSService");
            Service service = Service.create(url, qname);
            BOBWS BOBWS = service.getPort(BOBWS.class);

            List<Avance> avances = BOBWS.obtenerAvancesPorObra(idObra);
            model.addAttribute("avances", avances);
            model.addAttribute("idObra", idObra);

        } catch (Exception e) {
            model.addAttribute("error", "Error al conectar con el servicio: " + e.getMessage());
        }
        return "buscarAvance";
    }
}
