package com.example.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorInicio {

    @GetMapping("/")
    public String inicio() {
        return "index"; // Redirige a index.html
    }

    @GetMapping("/index")
    public String index() {
        return "index"; // Renderiza index.html
    }
}